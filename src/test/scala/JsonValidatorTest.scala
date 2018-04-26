import jsonvalidator.Validator

class JsonValidatorTest extends org.scalatest.FunSuite {
  test("isValidJson") {
    assert( Validator.isValidJson("{}") )
    assert( !Validator.isValidJson("{123}") )
  }
}
