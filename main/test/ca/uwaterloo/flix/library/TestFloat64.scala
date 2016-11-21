/*
 * Copyright 2016 Liam Palmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.uwaterloo.flix.library

import ca.uwaterloo.flix.api.Flix
import ca.uwaterloo.flix.util.Options
import org.scalatest.FunSuite

class TestFloat64 extends FunSuite {

  val options = Options.DefaultTest

  def runTest(input: String, output: Double) {
    val flix = new Flix().setOptions(options).addPath("main/src/library/Float64.flix").addStr(input)
    assertResult(output)(flix.solve().get.eval("r"))
  }

  // Used for testing equality with the Double.NaN type
  def runEqualsTest(input: String, output: Double) {
    val flix = new Flix().setOptions(options).addPath("main/src/library/Float64.flix").addStr(input)
    assert(output.equals(flix.solve().get.eval("r")))
  }

  test("size.01") {
    val input = "def r: Int32 = Float64/size()"
    runTest(input, 64)
  }

  test("minExponent.01") {
    val input = "def r: Int32 = Float64/minExponent()"
    runTest(input, -1022)
  }

  test("maxExponent.01") {
    val input = "def r: Int32 = Float64/maxExponent()"
    runTest(input, 1023)
  }

  test("maxValue.01") {
    val input = "def r: Float64 = Float64/maxValue()"
    runTest(input, Double.MaxValue)
  }

  test("minValue.01") {
    val input = "def r: Float64 = Float64/minValue()"
    runTest(input, Double.MinValue)
  }

  test("minPositiveValue.01") {
    val input = "def r: Float64 = Float64/minPositiveValue()"
    runTest(input, Double.MinPositiveValue)
  }

  test("undefined.01") {
    val input = "def r: Float64 = Float64/undefined()"
    runEqualsTest(input, Double.NaN)
  }

  test("positiveInfinity.01") {
    val input = "def r: Float64 = Float64/positiveInfinity()"
    runTest(input, Double.PositiveInfinity)
  }

  test("negativeInfinity.01") {
    val input = "def r: Float64 = Float64/negativeInfinity()"
    runTest(input, Double.NegativeInfinity)
  }

  test("min.01") {
    val input = "def r: Float64 = Float64/min(1.0f64, 2.0f64)"
    runTest(input, 1f)
  }

  test("min.02") {
    val input = "def r: Float64 = Float64/min(2.0f64, -1.0f64)"
    runTest(input, -1f)
  }

  test("min.03") {
    val input = "def r: Float64 = Float64/min(-33.0f64, -66.0f64)"
    runTest(input, -66f)
  }

  test("max.01") {
    val input = "def r: Float64 = Float64/max(48.0f64, 49.0f64)"
    runTest(input, 49f)
  }

  test("max.02") {
    val input = "def r: Float64 = Float64/max(4.0f64, -16.0f64)"
    runTest(input, 4f)
  }

  test("max.03") {
    val input = "def r: Float64 = Float64/max(-34.0f64, -16.0f64)"
    runTest(input, -16f)
  }
}