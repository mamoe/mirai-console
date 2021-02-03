/*
 * Copyright 2019-2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AFFERO GENERAL PUBLIC LICENSE version 3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.intellij.diagnostics

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReferenceExpression
import net.mamoe.mirai.console.compiler.common.castOrNull
import net.mamoe.mirai.console.intellij.diagnostics.ExternalResourceResolver.isCallingExternalResourceCreators
import net.mamoe.mirai.console.intellij.resolve.*
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.search.declarationsSearch.findDeepestSuperMethodsKotlinAware
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingElement
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.resolve
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

/*
private val bundle by lazy {
    BundleUtil.loadLanguageBundle(PluginMainServiceNotConfiguredInspection::class.java.classLoader, "messages.InspectionGadgetsBundle")!!
}*/

val CONTACT_FQ_NAME = FqName("net.mamoe.mirai.contact.Contact")
val CONTACT_COMPANION_FQ_NAME = FqName("net.mamoe.mirai.contact.Contact.Companion")

fun KtReferenceExpression.resolveCalleeFunction(): KtNamedFunction? {
    val originalCallee = getCalleeExpressionIfAny()?.castOrNull<KtReferenceExpression>()?.resolve() ?: return null
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

object ResourceNotClosedInspectionProcessors {
    val processors = arrayOf(
        FirstArgumentProcessor,
        KtExtensionProcessor
    )

    interface Processor {
        fun visitKtExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: KtCallExpression)
        fun visitPsiReferenceExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiReferenceExpression)
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
            if (signatures.none { callee.hasSignature(it) }) return
            if (!parent.receiverExpression.isCallingExternalResourceCreators()) return

            holder.registerResourceNotClosedProblem(parent.receiverExpression, parent)
        }

        override fun visitPsiReferenceExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiReferenceExpression) {
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

            holder.registerResourceNotClosedProblem(firstArgument, expr)
        }

        override fun visitPsiReferenceExpr(holder: ProblemsHolder, isOnTheFly: Boolean, expr: PsiReferenceExpression) {

        }
    }

    private fun ProblemsHolder.registerResourceNotClosedProblem(target: PsiElement, callExpr: KtExpression) {
        registerProblem(
            target,
            @Suppress("DialogTitleCapitalization") "资源未关闭",
            ProblemHighlightType.WARNING,
        )
    }
}

object ExternalResourceResolver {

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
}


class ResourceNotClosedInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor visitor@{ expression ->
            for (processor in ResourceNotClosedInspectionProcessors.processors) {
                processor.visitKtExpr(holder, isOnTheFly, expression)
            }
        }
    }
}