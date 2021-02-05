/*
 * Copyright 2019-2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AFFERO GENERAL PUBLIC LICENSE version 3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.intellij.diagnostics

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiCallExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import net.mamoe.mirai.console.intellij.resolve.FunctionSignature
import net.mamoe.mirai.console.intellij.resolve.allSuperTypes
import net.mamoe.mirai.console.intellij.resolve.explicitReceiverExpression
import net.mamoe.mirai.console.intellij.resolve.hasSignature
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix
import org.jetbrains.kotlin.idea.quickfix.KotlinCrossLanguageQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinReferenceImporter
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsKotlinAware
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

/*
private val bundle by lazy {
    BundleUtil.loadLanguageBundle(PluginMainServiceNotConfiguredInspection::class.java.classLoader, "messages.InspectionGadgetsBundle")!!
}*/


class ResourceNotClosedInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitCallExpression(callExpression: KtCallExpression) {
                for (processor in ResourceNotClosedInspectionProcessors.processors) {
                    processor.visitKtExpr(holder, isOnTheFly, callExpression)
                }
            }

            override fun visitElement(element: PsiElement) {
                if (element is PsiCallExpression) {
                    for (processor in ResourceNotClosedInspectionProcessors.processors) {
                        processor.visitPsiExpr(holder, isOnTheFly, element)
                    }
                }
            }
        }
    }
}

val CONTACT_FQ_NAME = FqName("net.mamoe.mirai.contact.Contact")
val CONTACT_COMPANION_FQ_NAME = FqName("net.mamoe.mirai.contact.Contact.Companion")

fun KtReferenceExpression.resolveCalleeFunction(): KtNamedFunction? {
    val originalCallee = getCalleeExpressionIfAny()?.referenceExpression()?.resolve() ?: return null
    if (originalCallee !is KtNamedFunction) return null

    return originalCallee
}

fun KtNamedFunction.findDeepestSuperFunction(): KtNamedFunction {
    this.forEachOverridingElement { superMember, overridingMember ->
        true
    }
    return findDeepestSuperMethodsKotlinAware(this).lastOrNull() as? KtNamedFunction ?: this
}

fun KtNamedFunction.isNamedMemberFunctionOf(className: String, functionName: String, extensionReceiver: String? = null): Boolean {
    if (extensionReceiver != null) {
        if (this.receiverTypeReference?.resolveReferencedType()?.getKotlinFqName()?.toString() != extensionReceiver) return false
    }
    return this.name == functionName && this.containingClassOrObject?.allSuperTypes?.any { it.getKotlinFqName()?.toString() == className } == true
}

@Suppress("DialogTitleCapitalization")
object ResourceNotClosedInspectionProcessors {
    val processors = arrayOf(
        FirstArgumentProcessor,
        KtExtensionProcessor
    )

    interface Processor {
        fun visitKtExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: KtCallExpression)
        fun visitPsiExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiCallExpression)
    }

    object KtExtensionProcessor : Processor {
        // net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImage(net.mamoe.mirai.utils.ExternalResource, C, kotlin.coroutines.Continuation<? super net.mamoe.mirai.message.MessageReceipt<? extends C>>)
        private val SEND_AS_IMAGE_TO = FunctionSignature {
            name("sendAsImageTo")
            dispatchReceiver("net.mamoe.mirai.utils.ExternalResource.Companion")
            extensionReceiver("net.mamoe.mirai.utils.ExternalResource")
        }
        private val UPLOAD_AS_IMAGE = FunctionSignature {
            name("uploadAsImage")
            dispatchReceiver("net.mamoe.mirai.utils.ExternalResource.Companion")
            extensionReceiver("net.mamoe.mirai.utils.ExternalResource")
        }

        private val signatures = arrayOf(
            SEND_AS_IMAGE_TO, UPLOAD_AS_IMAGE
        )

        override fun visitKtExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: KtCallExpression) {
            val parent = expr.parent
            if (parent !is KtDotQualifiedExpression) return
            val callee = expr.resolveCalleeFunction() ?: return

            if (!parent.receiverExpression.isCallingExternalResourceCreators()) return

            class Fix(private val functionName: String) : KotlinCrossLanguageQuickFixAction<KtDotQualifiedExpression>(parent), KotlinUniversalQuickFix {
                override fun getFamilyName(): String = FAMILY_NAME
                override fun getText(): String = "修复 $functionName"

                override fun invokeImpl(project: Project, editor: Editor?, file: PsiFile) {
                    if (editor == null) return
                    val thisExpr = element ?: return
                    val selectorText = thisExpr.selectorExpression?.text ?: return
                    val thisReceiverExpr = thisExpr.receiverExpression

                    val receiverInThisReceiverExpr = thisReceiverExpr.explicitReceiverExpression() ?: return

                    KotlinReferenceImporter().autoImportReferenceAtCursor(editor, file)
                    thisExpr.replace(KtPsiFactory(project).createExpression("${receiverInThisReceiverExpr.text}.$functionName($selectorText)"))
                }
            }

            when {
                callee.hasSignature(SEND_AS_IMAGE_TO) -> {
                    // RECEIVER.sendAsImageTo
                    holder.registerResourceNotClosedProblem(
                        parent.receiverExpression,
                        Fix("sendAsImageTo"),
                    )
                }
                callee.hasSignature(UPLOAD_AS_IMAGE) -> {
                    holder.registerResourceNotClosedProblem(
                        parent.receiverExpression,
                        Fix("uploadAsImage"),
                    )
                }
            }
        }

        override fun visitPsiExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiCallExpression) {
        }

    }

    object FirstArgumentProcessor : Processor {
        private val CONTACT_UPLOAD_IMAGE = FunctionSignature {
            name("uploadImage")
            dispatchReceiver("net.mamoe.mirai.contact.Contact")
            parameters("net.mamoe.mirai.utils.ExternalResource")
        }
        private val CONTACT_COMPANION_UPLOAD_IMAGE = FunctionSignature {
            name("uploadImage")
            extensionReceiver("net.mamoe.mirai.contact.Contact")
            parameters("net.mamoe.mirai.utils.ExternalResource")
        }

        private val CONTACT_COMPANION_SEND_IMAGE = FunctionSignature {
            name("sendImage")
            extensionReceiver("net.mamoe.mirai.contact.Contact")
            parameters("net.mamoe.mirai.utils.ExternalResource")
        }

        private val signatures = arrayOf(
            CONTACT_UPLOAD_IMAGE,
            CONTACT_COMPANION_UPLOAD_IMAGE,
            CONTACT_COMPANION_SEND_IMAGE
        )

        override fun visitKtExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: KtCallExpression) {
            val callee = expr.resolveCalleeFunction() ?: return
            if (signatures.none { callee.hasSignature(it) }) return

            val firstArgument = expr.valueArguments.firstOrNull() ?: return
            if (firstArgument.getArgumentExpression()?.isCallingExternalResourceCreators() != true) return

            holder.registerResourceNotClosedProblem(firstArgument)
        }

        override fun visitPsiExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiCallExpression) {

        }
    }

    private fun ProblemsHolder.registerResourceNotClosedProblem(target: PsiElement, vararg fixes: LocalQuickFix) {
        registerProblem(
            target,
            @Suppress("DialogTitleCapitalization") "资源未关闭",
            ProblemHighlightType.WARNING,
            *fixes
        )
    }
}

private val EXTERNAL_RESOURCE_CREATE = FunctionSignature {
    name("create")
    dispatchReceiver("net.mamoe.mirai.utils.ExternalResource")
}
private val TO_EXTERNAL_RESOURCE = FunctionSignature {
    name("toExternalResource")
    dispatchReceiver("net.mamoe.mirai.utils.ExternalResource.Companion")
}

fun KtExpression.isCallingExternalResourceCreators(): Boolean {
    val callExpr = resolveToCall(BodyResolveMode.PARTIAL)?.resultingDescriptor ?: return false
    return callExpr.hasSignature(EXTERNAL_RESOURCE_CREATE) || callExpr.hasSignature(TO_EXTERNAL_RESOURCE)
}

private const val FAMILY_NAME = "Mirai console"