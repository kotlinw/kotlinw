package kotlinw.util.stdlib

// TODO ez egy hibás implementáció, JS esetén nincs értelme, csak úgy, mint a ThreadLocal-nak sincs
actual class Lock {
    actual fun lock() {
    }

    actual fun unlock() {
    }
}
