package kotlinw.immutator.processor

import org.junit.jupiter.api.Test

class TestValidCases {
    @Test
    fun testKnownSimpleImmutableTypes() {
        checkCompilationResult(
            """
                        import kotlinw.immutator.annotation.Immutate
            
                        @Immutate
                        sealed interface TestCase {
                            val booleanProperty: Boolean
                            val booleanPropertyNullable: Boolean?
                            val charProperty: Char
                            val charPropertyNullable: Char?
                            val byteProperty: Byte
                            val bytePropertyNullable: Byte?
                            val shortProperty: Short
                            val shortPropertyNullable: Short?
                            val intProperty: Int
                            val intPropertyNullable: Int?
                            val longProperty: Long
                            val longPropertyNullable: Long?
                            val floatProperty: Float
                            val floatPropertyNullable: Float?
                            val doubleProperty: Doble
                            val doublePropertyNullable: Double?
                            val unitProperty: Unit
                            val unitPropertyNullable: Unit?
                            val stringProperty: String
                            val stringPropertyNullable: String?
                            val localDateProperty: kotlinx.datetime.LocalDate
                            val localDatePropertyNullable: kotlinx.datetime.LocalDate?
                            val localDateTimeProperty: kotlinx.datetime.LocalDateTime
                            val localDateTimePropertyNullable: kotlinx.datetime.LocalDateTime?
                            val instantProperty: kotlinx.datetime.Instant
                            val instantPropertyNullable: kotlinx.datetime.Instant?
                            val javaLocalDateProperty: java.time.LocalDate
                            val javaLocalDatePropertyNullable: java.time.LocalDate?
                            val javaLocalDateTimeProperty: java.time.LocalDateTime
                            val javaLocalDateTimePropertyNullable: java.time.LocalDateTime?
                            val javaInstantProperty: java.time.Instant
                            val javaInstantPropertyNullable: java.time.Instant?
                            val javaZonedDateTimeProperty: java.time.ZonedDateTime
                            val javaZonedDateTimePropertyNullable: java.time.ZonedDateTime?
                            val javaUuidProperty: java.util.UUID
                            val javaUuidPropertyNullable: java.util.UUID?
                        }
                        """
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testListOfKnownSimpleImmutableTypes() {
        checkCompilationResult(
            """
                        import kotlinw.immutator.annotation.Immutate
            
                        @Immutate
                        sealed interface TestCase {
                            val listOfBoolean: List<Boolean>
                            val nullableListOfBoolean: List<Boolean>?
                            val listOfBooleanNullable: List<Boolean?>
                            val nullableListOfBooleanNullable: List<Boolean?>?
                            
                            val listOfChar: List<Char>
                            val nullableListOfChar: List<Char>?
                            val listOfCharNullable: List<Char?>
                            val nullableListOfCharNullable: List<Char?>?
                            
                            val listOfByte: List<Byte>
                            val nullableListOfByte: List<Byte>?
                            val listOfByteNullable: List<Byte?>
                            val nullableListOfByteNullable: List<Byte?>?
                            
                            val listOfShort: List<Short>
                            val nullableListOfShort: List<Short>?
                            val listOfShortNullable: List<Short?>
                            val nullableListOfShortNullable: List<Short?>?
                            
                            val listOfInt: List<Int>
                            val nullableListOfInt: List<Int>?
                            val listOfIntNullable: List<Int?>
                            val nullableListOfIntNullable: List<Int?>?
                            
                            val listOfLong: List<Long>
                            val nullableListOfLong: List<Long>?
                            val listOfLongNullable: List<Long?>
                            val nullableListOfLongNullable: List<Long?>?

                            val listOfFloat: List<Float>
                            val nullableListOfFloat: List<Float>?
                            val listOfFloatNullable: List<Float?>
                            val nullableListOfFloatNullable: List<Float?>?

                            val listOfDouble: List<Double>
                            val nullableListOfDouble: List<Double>?
                            val listOfDoubleNullable: List<Double?>
                            val nullableListOfDoubleNullable: List<Double?>?

                            val listOfUnit: List<Unit>
                            val nullableListOfUnit: List<Unit>?
                            val listOfUnitNullable: List<Unit?>
                            val nullableListOfUnitNullable: List<Unit?>?

                            val listOfString: List<String>
                            val nullableListOfString: List<String>?
                            val listOfStringNullable: List<String?>
                            val nullableListOfStringNullable: List<String?>?

                            val listOfLocalDate: List<kotlinx.datetime.LocalDate>
                            val nullableListOfLocalDate: List<kotlinx.datetime.LocalDate>?
                            val listOfLocalDateNullable: List<kotlinx.datetime.LocalDate?>
                            val nullableListOfLocalDateNullable: List<kotlinx.datetime.LocalDate?>?

                            val listOfLocalDateTime: List<kotlinx.datetime.LocalDateTime>
                            val nullableListOfLocalDateTime: List<kotlinx.datetime.LocalDateTime>?
                            val listOfLocalDateTimeNullable: List<kotlinx.datetime.LocalDateTime?>
                            val nullableListOfLocalDateTimeNullable: List<kotlinx.datetime.LocalDateTime?>?

                            val listOfInstant: List<kotlinx.datetime.Instant>
                            val nullableListOfInstant: List<kotlinx.datetime.Instant>?
                            val listOfInstantNullable: List<kotlinx.datetime.Instant?>
                            val nullableListOfInstantNullable: List<kotlinx.datetime.Instant?>?

                            val listOfJavaLocalDate: List<java.time.LocalDate>
                            val nullableListOfJavaLocalDate: List<java.time.LocalDate>?
                            val listOfJavaLocalDateNullable: List<java.time.LocalDate?>
                            val nullableListOfJavaLocalDateNullable: List<java.time.LocalDate?>?

                            val listOfJavaLocalDateTime: List<java.time.LocalDateTime>
                            val nullableListOfJavaLocalDateTime: List<java.time.LocalDateTime>?
                            val listOfJavaLocalDateTimeNullable: List<java.time.LocalDateTime?>
                            val nullableListOfJavaLocalDateTimeNullable: List<java.time.LocalDateTime?>?

                            val listOfJavaInstant: List<java.time.Instant>
                            val nullableListOfJavaInstant: List<java.time.Instant>?
                            val listOfJavaInstantNullable: List<java.time.Instant?>
                            val nullableListOfJavaInstantNullable: List<java.time.Instant?>?

                            val listOfJavaZonedDateTime: List<java.time.LocalDateTime>
                            val nullableListOfJavaZonedDateTime: List<java.time.LocalDateTime>?
                            val listOfJavaZonedDateTimeNullable: List<java.time.LocalDateTime?>
                            val nullableListOfJavaZonedDateTimeNullable: List<java.time.LocalDateTime?>?

                            val listOfJavaUuid: List<java.util.UUID>
                            val nullableListOfJavaUuid: List<java.util.UUID>?
                            val listOfJavaUuidNullable: List<java.util.UUID?>
                            val nullableListOfJavaUuidNullable: List<java.util.UUID?>?
                        }
                        """
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testInferredImmutableDataClass() {
        checkCompilationResult(
            """
                        import kotlinw.immutator.annotation.Immutate
                        
                        data class Data(val s: String)
                                    
                        @Immutate
                        sealed interface TestCase {
                            val d: Data
                        }
                        """
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testInferredImmutableValueClass() {
        checkCompilationResult(
            """
                        import kotlinw.immutator.annotation.Immutate

                        value class Data(val s: String)

                        @Immutate
                        sealed interface TestCase {
                            val d: Data
                        }
                        """
        ) {
            assertCompilationSucceeded()
        }
    }

    @Test
    fun testInferredImmutability() {
        checkCompilationResult(
            """
                    import kotlinw.immutator.annotation.Immutate
                    import kotlinw.common.util.Uuid
                    
                    value class Data(val value: Uuid)
                    
                    @Immutate
                    sealed interface TestCase {
                        val d: Data
                    }
                    """
        ) {
            assertCompilationSucceeded()
        }
    }
}
