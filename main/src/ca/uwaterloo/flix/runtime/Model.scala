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

package ca.uwaterloo.flix.runtime

import ca.uwaterloo.flix.language.ast.{Time, ExecutableAst, Symbol}

/**
  * A class representing the minimal model.
  *
  * @param root        the abstract syntax tree of the program.
  * @param definitions the definitions in the program.
  * @param relations   the relational facts in the model.
  * @param lattices    the lattice facts in the model.
  */
class Model(root: ExecutableAst.Root,
            time: Time,
            definitions: Map[Symbol.DefnSym, () => AnyRef],
            relations: Map[Symbol.TableSym, Iterable[List[AnyRef]]],
            lattices: Map[Symbol.TableSym, Iterable[(List[AnyRef], AnyRef)]]) {

  // TODO: Hide this behind an interface, IModel?

  // TODO: Remove?
  def getRoot: ExecutableAst.Root = root

  // TODO: Remove?
  def getTime: Time = time

  @deprecated("to be removed", "0.1")
  def getConstant(sym: Symbol.DefnSym): AnyRef = definitions(sym)()

  // TODO: Needs to take arguments.
  def getConstant(name: String): AnyRef = getConstant(Symbol.mkDefnSym(name))

  // TODO: Should throw a java exception if the relation does not exist.
  def getRelation(name: String): Iterable[List[AnyRef]] =
  getRelationOpt(name).get

  @deprecated("to be removed", "0.1")
  def getRelationOpt(name: String): Option[Iterable[List[AnyRef]]] =
    relations.get(Symbol.mkTableSym(name))

  // TODO: Should throw a java exception if the lattice does not exist.
  def getLattice(name: String): Iterable[(List[AnyRef], AnyRef)] =
  getLatticeOpt(name).get

  @deprecated("to be removed", "0.1")
  def getLatticeOpt(name: String): Option[Iterable[(List[AnyRef], AnyRef)]] =
    lattices.get(Symbol.mkTableSym(name))

}