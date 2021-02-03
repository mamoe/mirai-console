/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.intellij.resolve

import net.mamoe.mirai.console.intellij.diagnostics.resolveReferencedType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.getReturnTypeReference
import org.jetbrains.kotlin.idea.refactoring.fqName.fqName
import org.jetbrains.kotlin.idea.search.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.postProcessing.type
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe

inline fun FunctionSignature(builderAction: FunctionSignatureBuilder.() -> Unit): FunctionSignature {
    return FunctionSignatureBuilder().apply(builderAction).build()
}

data class FunctionSignature(
    val name: String? = null,
    val dispatchReceiver: FqName? = null,
    val extensionReceiver: FqName? = null,
    val parameters: List<FqName>? = null,
    val returnType: FqName? = null,
)

class FunctionSignatureBuilder {
    private var name: String? = null
    private var dispatchReceiver: FqName? = null
    private var extensionReceiver: FqName? = null
    private var parameters: List<FqName>? = null
    private var returnType: FqName? = null

    fun name(name: String) {
        this.name = name
    }

    fun dispatchReceiver(dispatchReceiver: String) {
        this.dispatchReceiver = FqName(dispatchReceiver)
    }

    fun extensionReceiver(extensionReceiver: String) {
        this.extensionReceiver = FqName(extensionReceiver)
    }

    fun parameters(vararg parameters: String) {
        this.parameters = parameters.map { FqName(it) }
    }

    fun returnType(returnType: String) {
        this.returnType = FqName(returnType)
    }

    fun build(): FunctionSignature = FunctionSignature(name, dispatchReceiver, extensionReceiver, parameters, returnType)
}


fun KtNamedFunction.hasSignature(functionSignature: FunctionSignature): Boolean {
    if (functionSignature.name != null) {
        if (this.name != functionSignature.name) return false
    }
    if (functionSignature.extensionReceiver != null) {
        if (this.receiverTypeReference?.resolveReferencedType()?.getKotlinFqName() != functionSignature.extensionReceiver) return false
    }
    if (functionSignature.dispatchReceiver != null) {
        if (this.containingClassOrObject?.fqName != functionSignature.dispatchReceiver) return false
    }
    if (functionSignature.parameters != null) {
        if (this.valueParameters.zip(functionSignature.parameters).any { it.first.type()?.fqName != it.second }) return false
    }
    if (functionSignature.returnType != null) {
        if (this.getReturnTypeReference()?.resolveReferencedType()?.getKotlinFqName() != functionSignature.returnType) return false
    }
    return true
}

fun CallableDescriptor.hasSignature(functionSignature: FunctionSignature): Boolean {
    if (functionSignature.name != null) {
        if (this.name.toString() != functionSignature.name) return false
    }
    if (functionSignature.extensionReceiver != null) {
        if (this.extensionReceiverParameter?.fqNameUnsafe != functionSignature.extensionReceiver.toUnsafe()) return false
    }
    if (functionSignature.dispatchReceiver != null) {
        if (this.containingDeclaration.fqNameUnsafe != functionSignature.dispatchReceiver.toUnsafe()) return false
    }
    if (functionSignature.parameters != null) {
        if (this.valueParameters.zip(functionSignature.parameters).any { it.first.type.fqName != it.second }) return false
    }
    if (functionSignature.returnType != null) {
        if (this.returnType?.fqName != functionSignature.returnType) return false
    }
    return true
}