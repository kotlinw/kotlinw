package kotlinw.util.stdlib

inline fun infiniteLoop(loopBlock: () -> Unit): Nothing {
    while (true) {
        loopBlock()
    }
}
