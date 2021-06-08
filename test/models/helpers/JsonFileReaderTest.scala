package models.helpers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JsonFileReaderTest extends AnyFlatSpec with Matchers {

  "JsonFileReader" should "read json contents to specified type" in {

    val data = """["foo", "bar"]"""

    val reader = new JsonFileReader

    val parsedData: List[String] = reader.parse[List[String]](data)
    parsedData should be(List("foo", "bar"))
  }

  it should "read data from file" in {

    val reader = new JsonFileReader

    val data = reader.read("/models/helpers/listOfStrings.json")

    data should be("""["foo","bar"]""")
  }

  it should "read data from file and parse it to a valid type" in {
    val reader = new JsonFileReader
    val parsedData = reader.readAndParse[List[String]]("/models/helpers/listOfStrings.json")
    parsedData should be(List("foo", "bar"))
  }

}
