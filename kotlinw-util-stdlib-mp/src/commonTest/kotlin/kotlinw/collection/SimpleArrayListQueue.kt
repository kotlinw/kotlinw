package kotlinw.collection

class SimpleArrayListQueue private constructor(
    private val list: ArrayList<Int>
) :
    MutableQueue<Int>, MutableCollection<Int> by list {

    constructor() : this(ArrayList())

    override fun enqueue(element: Int) {
        add(element)
    }

    override fun dequeueOrNull(): Int? = list.removeFirstOrNull()

    override fun peekOrNull(): Int? = list.firstOrNull()
}
