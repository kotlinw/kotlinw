# Bloom filter

```kotlin
with(newMutableBloomFilter<Int>(4)) {

    assertFalse(mightContain(0))
    assertFalse(mightContain(1))
    assertFalse(mightContain(2))
    assertFalse(mightContain(3))

    add(0)

    assertTrue(mightContain(0))
    assertFalse(mightContain(1))
    assertFalse(mightContain(2))
    assertFalse(mightContain(3))

    add(1)

    assertTrue(mightContain(0))
    assertTrue(mightContain(1))
    assertFalse(mightContain(2))
    assertFalse(mightContain(3))

    add(2)

    assertTrue(mightContain(0))
    assertTrue(mightContain(1))
    assertTrue(mightContain(2))
    assertFalse(mightContain(3))

    add(3)

    assertTrue(mightContain(0))
    assertTrue(mightContain(1))
    assertTrue(mightContain(2))
    assertTrue(mightContain(3))

    (4..Int.MAX_VALUE).forEach {
        mightContain(it)
    }
}
```
