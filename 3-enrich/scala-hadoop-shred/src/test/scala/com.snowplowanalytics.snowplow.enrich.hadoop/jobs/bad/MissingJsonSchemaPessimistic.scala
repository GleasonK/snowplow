/*
 * Copyright (c) 2014 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich
package hadoop
package jobs
package bad

// Specs2
import org.specs2.mutable.Specification

// Scalding
import com.twitter.scalding._

// Cascading
import cascading.tuple.TupleEntry
import cascading.tap.SinkMode

// This project
import JobSpecHelpers._

/**
 * Holds the input data for the test,
 * plus a lambda to create the expected
 * output.
 */
object MissingJonsSchemaPessimistic {

  val lines = Lines(
    "TODO"
    )

  val expected = (line: String) => s"""{"line":"${line}","errors":["{"level":"error","message":"Line does not match Snowplow enriched event (expected 104 fields; found 1)"}"]}""".format(line)
}

/**
 * Integration test for the EtlJob:
 *
 * Input data is _not_ in the expected
 * Snowplow enriched event format.
 */
class MissingJonsSchemaPessimistic extends Specification {

  import Dsl._

  "A job which cannot find the specified JSON Schemas in Iglu" should {
    ShredJobSpec.
      source(MultipleTextLineFiles("inputFolder"), MissingJonsSchemaPessimistic.lines).
      sink[String](PartitionedTsv("outputFolder", ShredJob.ShreddedPartition, false, ('json), SinkMode.REPLACE)){ output =>
        "not write any events" in {
          output must beEmpty
        }
      }.
      sink[TupleEntry](Tsv("exceptionsFolder")){ trap =>
        "not trap any exceptions" in {
          trap must beEmpty
        }
      }.
      sink[String](Tsv("badFolder")){ json =>
        "write a bad row JSON with input line and error message for each missing schema" in {
          for (i <- json.indices) {
            json(i) must_== MissingJonsSchemaPessimistic.expected(MissingJonsSchemaPessimistic.lines(i)._2)
          }
        }
      }.
      run.
      finish
  }
}