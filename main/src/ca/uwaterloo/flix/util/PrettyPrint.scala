/*
 * Copyright 2015-2016 Magnus Madsen
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

package ca.uwaterloo.flix.util

import ca.uwaterloo.flix.api.IModel
import ca.uwaterloo.flix.api.RichDSL._
import ca.uwaterloo.flix.language.ast.{ExecutableAst, Symbol}
import ca.uwaterloo.flix.runtime.Value

object PrettyPrint {

  def print(name: String, model: IModel): Unit = {
    val sym = Symbol.mkTableSym(name)

    var found = false

    model.getRelationOpt(name) match {
      case None => // nop
      case Some(relation) =>
        val r = model.getRoot.tables(sym).asInstanceOf[ExecutableAst.Table.Relation]
        val cols = r.attributes.map(_.name)
        val ascii = new AsciiTable().withCols(cols: _*)
        for (row <- relation.toSeq.sortBy(_.head.toString)) {
          ascii.mkRow(row map Value.pretty)
        }

        Console.println(r.sym)
        ascii.write(System.out)
        Console.println()
        Console.println()
        found = true
    }

    model.getLatticeOpt(name) match {
      case None => // nop
      case Some(xs) =>
        val l = model.getRoot.tables(sym).asInstanceOf[ExecutableAst.Table.Lattice]
        val cols = l.keys.map(_.name).toList ::: l.value.name :: Nil
        val ascii = new AsciiTable().withCols(cols: _*)
        for ((keys, elm) <- xs.toSeq.sortBy(_._1.head.toString)) {
          ascii.mkRow((keys map Value.pretty) ++ List(Value.pretty(elm)))
        }

        Console.println(l.sym)
        ascii.write(System.out)
        Console.println()
        Console.println()
        found = true
    }

    if (!found)
      Console.println("No such name: " + name)
  }

}
