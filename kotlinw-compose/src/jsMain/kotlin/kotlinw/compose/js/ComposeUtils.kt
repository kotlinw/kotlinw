package kotlinw.compose.js

import kotlinx.browser.document
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.AttrsScopeBuilder
import org.jetbrains.compose.web.dom.ElementBuilder
import org.w3c.dom.Element

open class ElementBuilderImplementation<TElement : Element>(private val tagName: String) :
    ElementBuilder<TElement> {
    private val el: Element by lazy { document.createElement(tagName) }
    override fun create(): TElement = el.cloneNode() as TElement
}

value class SlotName(val value: String)

interface HasSlotName {
    val slotName: SlotName
}

fun <TElement : Element> AttrsScopeBuilder<TElement>.assignToSlot(slotName: SlotName): AttrsScope<TElement> =
    attr("slot", slotName.value)

fun <TElement : Element> AttrsScopeBuilder<TElement>.assignToSlot(slotNameProvider: HasSlotName): AttrsScope<TElement> =
    assignToSlot(slotNameProvider.slotName)
