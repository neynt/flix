package ca.uwaterloo.flix.runtime

import ca.uwaterloo.flix.language.ast.TypedAst.{Definition, Expression, Literal, Pattern, Type, FormalArg, Root}
import ca.uwaterloo.flix.language.ast.{BinaryOperator, UnaryOperator, ParsedAst, Name, SourceLocation}
import org.scalatest.FunSuite

class TestInterpreter extends FunSuite {
  val root = Root(Map(), Map(), Map(), List(), List())

  val loc = SourceLocation(None, 0, 0)

  val ident01 = Name.Ident("x", loc)
  val ident02 = Name.Ident("y", loc)
  val ident03 = Name.Ident("z", loc)

  object ConstantPropTagDefs {
    val name = Name.Resolved(List("ConstProp"))
    val identB = Name.Ident("Bot", loc)
    val identV = Name.Ident("Val", loc)
    val identT = Name.Ident("Top", loc)

    val tagTpeB = Type.Tag(name, identB, Type.Unit)
    val tagTpeV = Type.Tag(name, identV, Type.Int)
    val tagTpeT = Type.Tag(name, identT, Type.Unit)
    val enumTpe = Type.Enum(Map("ConstProp.Bot" -> tagTpeB, "ConstProp.Val" -> tagTpeV, "ConstProp.Top" -> tagTpeT))
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Literals                                                  //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Literal.Unit") {
    val input = Expression.Lit(Literal.Unit(loc), Type.Unit)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Unit)(result)
  }

  test("Interpreter - Literal.Bool01") {
    val input = Expression.Lit(Literal.Bool(true, loc), Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - Literal.Bool02") {
    val input = Expression.Lit(Literal.Bool(false, loc), Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Literal.Int01") {
    val input = Expression.Lit(Literal.Int(-242, loc), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-242))(result)
  }

  test("Interpreter - Literal.Int02") {
    val input = Expression.Lit(Literal.Int(-42, loc), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-42))(result)
  }

  test("Interpreter - Literal.Int03") {
    val input = Expression.Lit(Literal.Int(0, loc), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - Literal.Int04") {
    val input = Expression.Lit(Literal.Int(98, loc), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(98))(result)
  }

  test("Interpreter - Literal.Int05") {
    val input = Expression.Lit(Literal.Int(91238, loc), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(91238))(result)
  }

  test("Interpreter - Literal.Str01") {
    val input = Expression.Lit(Literal.Str("", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str(""))(result)
  }

  test("Interpreter - Literal.Str02") {
    val input = Expression.Lit(Literal.Str("Hello World!", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("Hello World!"))(result)
  }

  test("Interpreter - Literal.Str03") {
    val input = Expression.Lit(Literal.Str("asdf", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("asdf"))(result)
  }

  test("Interpreter - Literal.Str04") {
    val input = Expression.Lit(Literal.Str("foobar", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("foobar"))(result)
  }

  test("Interpreter - Literal.Str05") {
    val input = Expression.Lit(Literal.Str("\"\"\"", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("\"\"\""))(result)
  }

  test("Interpreter - Literal.Tuple01") {
    val input = Expression.Lit(
      Literal.Tuple(List(Literal.Int(42, loc), Literal.Bool(false, loc), Literal.Str("hi", loc)),
        Type.Tuple(List(Type.Int, Type.Bool, Type.Str)), loc),
      Type.Tuple(List(Type.Int, Type.Bool, Type.Str)))
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tuple(List(Value.Int(42), Value.Bool(false), Value.Str("hi"))))(result)
  }

  test("Interpreter - Literal.Tuple02") {
    val input = Expression.Lit(
      Literal.Tuple(List(
        Literal.Int(4, loc),
        Literal.Tuple(List(Literal.Int(12, loc), Literal.Int(8, loc)),
          Type.Tuple(List(Type.Int, Type.Int)), loc)),
        Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int)))), loc),
      Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int)))))
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tuple(List(Value.Int(4), Value.Tuple(List(Value.Int(12), Value.Int(8))))))(result)
  }

  test("Interpreter - Literal.Tag01") {
    val name = Name.Resolved(List("foo", "bar"))
    val ident = Name.Ident("baz", loc)
    val tagTpe = Type.Tag(name, ident, Type.Str)
    val enumTpe = Type.Enum(Map("foo.bar.baz" -> tagTpe))
    val input = Expression.Lit(Literal.Tag(name, ident, Literal.Str("hello world", loc), enumTpe, loc), tagTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "baz", Value.Str("hello world")))(result)
  }

  test("Interpreter - Literal.Tag02") {
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val input = Expression.Lit(Literal.Tag(name, ident,
      Literal.Tuple(List(Literal.Str("James", loc), Literal.Int(42, loc)),
        Type.Tuple(List(Type.Str, Type.Int)), loc), enumTpe, loc), tagTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "NameAndAge", Value.Tuple(List(Value.Str("James"), Value.Int(42)))))(result)
  }

  test("Interpreter - Literal.Tag03") {
    import ConstantPropTagDefs._
    val input = Expression.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Bot", Value.Unit))(result)
  }

  test("Interpreter - Literal.Tag04") {
    import ConstantPropTagDefs._
    val input = Expression.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Top", Value.Unit))(result)
  }

  test("Interpreter - Literal.Tag05") {
    import ConstantPropTagDefs._
    val input = Expression.Lit(Literal.Tag(name, identV, Literal.Int(0, loc), enumTpe, loc), tagTpeV)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(0)))(result)

  }

  test("Interpreter - Literal.Tag06") {
    import ConstantPropTagDefs._
    val input = Expression.Lit(Literal.Tag(name, identV, Literal.Int(-240, loc), enumTpe, loc), tagTpeV)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(-240)))(result)
  }

  test("Interpreter - Literal.Tag07") {
    import ConstantPropTagDefs._
    val input = Expression.Lit(Literal.Tag(name, identV, Literal.Int(1241, loc), enumTpe, loc), tagTpeV)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(1241)))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Var                                                       //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Var01") {
    val input = Expression.Lit(Literal.Str("hello", loc), Type.Str)
    val env = Map(ident01 -> Value.Bool(false))
    val result = Interpreter.eval(input, root, env)
    assertResult(Value.Str("hello"))(result)
  }

  test("Interpreter - Expression.Var02") {
    val input = Expression.Var(ident01, Type.Int)
    val env = Map(ident01 -> Value.Int(5))
    val result = Interpreter.eval(input, root, env)
    assertResult(Value.Int(5))(result)
  }

  test("Interpreter - Expression.Var03") {
    val input = Expression.Var(ident01, Type.Bool)
    val env = Map(ident01 -> Value.Bool(false))
    val result = Interpreter.eval(input, root, env)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Expression.Var04") {
    val input = Expression.Var(ident02, Type.Str)
    val env = Map(ident01 -> Value.Str("foo"), ident02 -> Value.Str("bar"))
    val result = Interpreter.eval(input, root, env)
    assertResult(Value.Str("bar"))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Ref                                                       //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Ref01") {
    val name = Name.Resolved(List("foo", "bar", "baz"))
    val const = Definition.Constant(name, Expression.Lit(Literal.Bool(false, loc), Type.Bool), Type.Bool)
    val root = Root(Map(name -> const), Map(), Map(), List(), List())
    val input = Expression.Lit(Literal.Str("hello", loc), Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("hello"))(result)
  }

  test("Interpreter - Expression.Ref02") {
    val name = Name.Resolved(List("foo", "bar", "baz"))
    val const = Definition.Constant(name, Expression.Lit(Literal.Int(5, loc), Type.Int), Type.Int)
    val root = Root(Map(name -> const), Map(), Map(), List(), List())
    val input = Expression.Ref(name, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(5))(result)
  }

  test("Interpreter - Expression.Ref03") {
    val name = Name.Resolved(List("foo", "bar", "baz"))
    val const = Definition.Constant(name, Expression.Lit(Literal.Bool(false, loc), Type.Bool), Type.Bool)
    val root = Root(Map(name -> const), Map(), Map(), List(), List())
    val input = Expression.Ref(name, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Expression.Ref04") {
    val name01 = Name.Resolved(List("foo", "bar", "baz"))
    val name02 = Name.Resolved(List("abc", "def", "ghi"))
    val const01 = Definition.Constant(name01, Expression.Lit(Literal.Str("foo", loc), Type.Str), Type.Str)
    val const02 = Definition.Constant(name01, Expression.Lit(Literal.Str("bar", loc), Type.Str), Type.Str)
    val root = Root(Map(name01 -> const01, name02 -> const02), Map(), Map(), List(), List())
    val input = Expression.Ref(name02, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("bar"))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Lambda and Apply                                          //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Lambda01") {
    // () => false
    val lambda = Expression.Lambda(
      List(), Expression.Lit(Literal.Bool(false, loc), Type.Bool), Type.Function(List(), Type.Bool))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (() => false)()
    val apply = Expression.Apply(lambda, List(), Type.Bool)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Bool(false))(value)
  }

  test("Interpreter - Expression.Lambda02") {
    // x => 3
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int)), Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Function(List(Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x => 3)(4)
    val apply = Expression.Apply(lambda, List(Expression.Lit(Literal.Int(4, loc), Type.Int)), Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(3))(value)
  }

  test("Interpreter - Expression.Lambda03") {
    // x => x
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int)), Expression.Var(ident01, Type.Int),
      Type.Function(List(Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x => x)(5)
    val apply = Expression.Apply(lambda, List(Expression.Lit(Literal.Int(5, loc), Type.Int)), Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(5))(value)
  }

  test("Interpreter - Expression.Lambda04") {
    // x => 1 + 2
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int)),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Lit(Literal.Int(1, loc), Type.Int),
        Expression.Lit(Literal.Int(2, loc), Type.Int),
        Type.Int),
      Type.Function(List(Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x => 1 + 2)(42)
    val apply = Expression.Apply(lambda, List(Expression.Lit(Literal.Int(42, loc), Type.Int)), Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(3))(value)
  }

  test("Interpreter - Expression.Lambda05") {
    // x => x + 2
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int)),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int),
        Expression.Lit(Literal.Int(2, loc), Type.Int),
        Type.Int),
      Type.Function(List(Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x => x + 2)(100)
    val apply = Expression.Apply(lambda, List(Expression.Lit(Literal.Int(100, loc), Type.Int)), Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(102))(value)
  }

  test("Interpreter - Expression.Lambda06") {
    // (x, y) => x + y
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int), FormalArg(ident02, Type.Int)),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int),
        Expression.Var(ident02, Type.Int),
        Type.Int),
      Type.Function(List(Type.Int, Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x, y => x + y)(3, 4)
    val apply = Expression.Apply(lambda, List(
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(4, loc), Type.Int)),
      Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(7))(value)
  }

  test("Interpreter - Expression.Lambda07") {
    // (x, y) => if (x) then true else y
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Bool), FormalArg(ident02, Type.Bool)),
      Expression.IfThenElse(
        Expression.Var(ident01, Type.Bool),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool),
        Expression.Var(ident02, Type.Bool),
        Type.Bool),
      Type.Function(List(Type.Bool, Type.Bool), Type.Bool))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // ((x, y) => if (x) then true else y)(false, true)
    val apply = Expression.Apply(lambda, List(
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      Type.Bool)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Bool(true))(value)
  }

  test("Interpreter - Expression.Lambda08") {
    // (x, y, z) => x + (y + z)
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int), FormalArg(ident02, Type.Int), FormalArg(ident03, Type.Int)),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int),
        Expression.Binary(
          BinaryOperator.Plus,
          Expression.Var(ident02, Type.Int),
          Expression.Var(ident03, Type.Int),
          Type.Int),
        Type.Int),
      Type.Function(List(Type.Int, Type.Int, Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x, y, z => x + (y + z))(2, 42, 5)
    val apply = Expression.Apply(lambda, List(
      Expression.Lit(Literal.Int(2, loc), Type.Int),
      Expression.Lit(Literal.Int(42, loc), Type.Int),
      Expression.Lit(Literal.Int(5, loc), Type.Int)),
      Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(49))(value)
  }

  test("Interpreter - Expression.Lambda09") {
    // x => (y => x + y)
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Int)),
      Expression.Lambda(
        List(FormalArg(ident02, Type.Int)),
        Expression.Binary(
          BinaryOperator.Plus,
          Expression.Var(ident01, Type.Int),
          Expression.Var(ident02, Type.Int),
          Type.Int),
        Type.Function(List(Type.Int), Type.Int)),
      Type.Function(List(Type.Int), Type.Function(List(Type.Int), Type.Int)))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x => (y => x + y)(3)(4)
    val apply = Expression.Apply(
      Expression.Apply(lambda, List(Expression.Lit(Literal.Int(3, loc), Type.Int)), Type.Int),
      List(Expression.Lit(Literal.Int(4, loc), Type.Int)),
      Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(7))(value)
  }

  test("Interpreter - Expression.Lambda10") {
    // x, y => x(y)
    val lambda = Expression.Lambda(
      List(FormalArg(ident01, Type.Function(List(Type.Int), Type.Int)), FormalArg(ident02, Type.Int)),
      Expression.Apply(
        Expression.Var(ident01, Type.Function(List(Type.Int), Type.Int)),
        List(Expression.Var(ident02, Type.Int)),
        Type.Int),
      Type.Function(List(Type.Function(List(Type.Int), Type.Int), Type.Int), Type.Int))
    val expected = Value.Closure(lambda.args, lambda.body, Map())
    val closure = Interpreter.eval(lambda, root)
    assertResult(expected)(closure)

    // (x, y => x(y))((x => x + 1), 5)
    val apply = Expression.Apply(lambda, List(
      Expression.Lambda(
        List(FormalArg(ident01, Type.Int)),
        Expression.Binary(
          BinaryOperator.Plus,
          Expression.Var(ident01, Type.Int),
          Expression.Lit(Literal.Int(1, loc), Type.Int),
          Type.Int),
        Type.Function(List(Type.Int), Type.Int)),
      Expression.Lit(Literal.Int(5, loc), Type.Int)),
      Type.Int)
    val value = Interpreter.eval(apply, root)
    assertResult(Value.Int(6))(value)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Unary and Binary                                          //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - UnaryOperator.Not01") {
    val input = Expression.Unary(
      UnaryOperator.Not,
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - UnaryOperator.Not02") {
    val input = Expression.Unary(
      UnaryOperator.Not,
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - UnaryOperator.UnaryPlus01") {
    val input = Expression.Unary(
      UnaryOperator.UnaryPlus,
      Expression.Lit(Literal.Int(23, loc), Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(23))(result)
  }

  test("Interpreter - UnaryOperator.UnaryPlus02") {
    val input = Expression.Unary(
      UnaryOperator.UnaryPlus,
      Expression.Lit(Literal.Int(-4, loc), Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-4))(result)
  }

  test("Interpreter - UnaryOperator.UnaryMinus03") {
    val input = Expression.Unary(
      UnaryOperator.UnaryMinus,
      Expression.Lit(Literal.Int(23, loc), Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-23))(result)
  }

  test("Interpreter - UnaryOperator.UnaryMinus02") {
    val input = Expression.Unary(
      UnaryOperator.UnaryMinus,
      Expression.Lit(Literal.Int(-4, loc), Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(4))(result)
  }

  test("Interpreter - BinaryOperator.Plus01") {
    val input = Expression.Binary(
      BinaryOperator.Plus,
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(500))(result)
  }

  test("Interpreter - BinaryOperator.Plus02") {
    val input = Expression.Binary(
      BinaryOperator.Plus,
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(500))(result)
  }

  test("Interpreter - BinaryOperator.Plus03") {
    val input = Expression.Binary(
      BinaryOperator.Plus,
      Expression.Lit(Literal.Int(-400, loc), Type.Int),
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-300))(result)
  }

  test("Interpreter - BinaryOperator.Plus04") {
    val input = Expression.Binary(
      BinaryOperator.Plus,
      Expression.Lit(Literal.Int(-100, loc), Type.Int),
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(300))(result)
  }

  test("Interpreter - BinaryOperator.Plus05") {
    val input = Expression.Binary(
      BinaryOperator.Plus,
      Expression.Lit(Literal.Int(-400, loc), Type.Int),
      Expression.Lit(Literal.Int(-100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-500))(result)
  }

  test("Interpreter - BinaryOperator.Minus01") {
    val input = Expression.Binary(
      BinaryOperator.Minus,
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(300))(result)
  }

  test("Interpreter - BinaryOperator.Minus02") {
    val input = Expression.Binary(
      BinaryOperator.Minus,
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-300))(result)
  }

  test("Interpreter - BinaryOperator.Minus03") {
    val input = Expression.Binary(
      BinaryOperator.Minus,
      Expression.Lit(Literal.Int(-400, loc), Type.Int),
      Expression.Lit(Literal.Int(100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-500))(result)
  }

  test("Interpreter - BinaryOperator.Minus04") {
    val input = Expression.Binary(
      BinaryOperator.Minus,
      Expression.Lit(Literal.Int(-100, loc), Type.Int),
      Expression.Lit(Literal.Int(400, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-500))(result)
  }

  test("Interpreter - BinaryOperator.Minus05") {
    val input = Expression.Binary(
      BinaryOperator.Minus,
      Expression.Lit(Literal.Int(-400, loc), Type.Int),
      Expression.Lit(Literal.Int(-100, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-300))(result)
  }

  test("Interpreter - BinaryOperator.Times01") {
    val input = Expression.Binary(
      BinaryOperator.Times,
      Expression.Lit(Literal.Int(2, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(6))(result)
  }

  test("Interpreter - BinaryOperator.Times02") {
    val input = Expression.Binary(
      BinaryOperator.Times,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(2, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(6))(result)
  }

  test("Interpreter - BinaryOperator.Times03") {
    val input = Expression.Binary(
      BinaryOperator.Times,
      Expression.Lit(Literal.Int(-2, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-6))(result)
  }

  test("Interpreter - BinaryOperator.Times04") {
    val input = Expression.Binary(
      BinaryOperator.Times,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(2, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-6))(result)
  }

  test("Interpreter - BinaryOperator.Times05") {
    val input = Expression.Binary(
      BinaryOperator.Times,
      Expression.Lit(Literal.Int(-2, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(6))(result)
  }

  test("Interpreter - BinaryOperator.Divide01") {
    val input = Expression.Binary(
      BinaryOperator.Divide,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(4))(result)
  }

  test("Interpreter - BinaryOperator.Divide02") {
    val input = Expression.Binary(
      BinaryOperator.Divide,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - BinaryOperator.Divide03") {
    val input = Expression.Binary(
      BinaryOperator.Divide,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-4))(result)
  }

  test("Interpreter - BinaryOperator.Divide04") {
    val input = Expression.Binary(
      BinaryOperator.Divide,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - BinaryOperator.Divide05") {
    val input = Expression.Binary(
      BinaryOperator.Divide,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(4))(result)
  }

  test("Interpreter - BinaryOperator.Modulo01") {
    val input = Expression.Binary(
      BinaryOperator.Modulo,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(2, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - BinaryOperator.Modulo02") {
    val input = Expression.Binary(
      BinaryOperator.Modulo,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(5, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(2))(result)
  }

  test("Interpreter - BinaryOperator.Modulo03") {
    val input = Expression.Binary(
      BinaryOperator.Modulo,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(5, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-2))(result)
  }

  test("Interpreter - BinaryOperator.Modulo04") {
    val input = Expression.Binary(
      BinaryOperator.Modulo,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(-5, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(2))(result)
  }

  test("Interpreter - BinaryOperator.Modulo05") {
    val input = Expression.Binary(
      BinaryOperator.Modulo,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-5, loc), Type.Int),
      Type.Int
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(-2))(result)
  }

  test("Interpreter - BinaryOperator.Less01") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Less02") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Less03") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Less04") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Less05") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Less06") {
    val input = Expression.Binary(
      BinaryOperator.Less,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual01") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual02") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual03") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual04") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual05") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.LessEqual06") {
    val input = Expression.Binary(
      BinaryOperator.LessEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Greater01") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Greater02") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Greater03") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Greater04") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Greater05") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Greater06") {
    val input = Expression.Binary(
      BinaryOperator.Greater,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual01") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual02") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual03") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual04") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual05") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.GreaterEqual06") {
    val input = Expression.Binary(
      BinaryOperator.GreaterEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Equal01") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Equal02") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Equal03") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Equal04") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Equal05") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Equal06") {
    val input = Expression.Binary(
      BinaryOperator.Equal,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual01") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual02") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual03") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Expression.Lit(Literal.Int(3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual04") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual05") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-12, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.NotEqual06") {
    val input = Expression.Binary(
      BinaryOperator.NotEqual,
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Expression.Lit(Literal.Int(-3, loc), Type.Int),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.And01") {
    val input = Expression.Binary(
      BinaryOperator.And,
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.And02") {
    val input = Expression.Binary(
      BinaryOperator.And,
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.And03") {
    val input = Expression.Binary(
      BinaryOperator.And,
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - BinaryOperator.Or01") {
    val input = Expression.Binary(
      BinaryOperator.Or,
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Or02") {
    val input = Expression.Binary(
      BinaryOperator.Or,
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - BinaryOperator.Or03") {
    val input = Expression.Binary(
      BinaryOperator.Or,
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Type.Bool
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - If Then Else                                              //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.IfThenElse01") {
    val input = Expression.IfThenElse(
      Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Str("foo", loc), Type.Str),
      Expression.Lit(Literal.Str("bar", loc), Type.Str),
      Type.Str
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("foo"))(result)
  }

  test("Interpreter - Expression.IfThenElse02") {
    val input = Expression.IfThenElse(
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.Lit(Literal.Str("foo", loc), Type.Str),
      Expression.Lit(Literal.Str("bar", loc), Type.Str),
      Type.Str
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("bar"))(result)
  }

  test("Interpreter - Expression.IfThenElse03") {
    // if (20 % 7 >= 3 || 25 - 5 == 4) "foo" else "bar"
    val input = Expression.IfThenElse(
      Expression.Binary(
        BinaryOperator.Or,
        Expression.Binary(
          BinaryOperator.GreaterEqual,
          Expression.Binary(
            BinaryOperator.Modulo,
            Expression.Lit(Literal.Int(20, loc), Type.Int),
            Expression.Lit(Literal.Int(7, loc), Type.Int),
            Type.Int
          ),
          Expression.Lit(Literal.Int(3, loc), Type.Int),
          Type.Bool
        ),
        Expression.Binary(
          BinaryOperator.Equal,
          Expression.Binary(
            BinaryOperator.Minus,
            Expression.Lit(Literal.Int(25, loc), Type.Int),
            Expression.Lit(Literal.Int(5, loc), Type.Int),
            Type.Int
          ),
          Expression.Lit(Literal.Int(4, loc), Type.Int),
          Type.Bool
        ),
        Type.Bool
      ),
      Expression.Lit(Literal.Str("foo", loc), Type.Str),
      Expression.Lit(Literal.Str("bar", loc), Type.Str),
      Type.Str
    )
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("foo"))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Let                                                       //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Let01") {
    // let x = true in 42
    val input = Expression.Let(ident01, Expression.Lit(Literal.Bool(true, loc), Type.Bool),
      Expression.Lit(Literal.Int(42, loc), Type.Int), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(42))(result)
  }

  test("Interpreter - Expression.Let02") {
    // let x = 24 in x
    val input = Expression.Let(ident01, Expression.Lit(Literal.Int(24, loc), Type.Int),
      Expression.Var(ident01, Type.Int), Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(24))(result)
  }

  test("Interpreter - Expression.Let03") {
    // let x = 1 in x + 2
    val input = Expression.Let(ident01, Expression.Lit(Literal.Int(1, loc), Type.Int),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int),
        Expression.Lit(Literal.Int(2, loc), Type.Int),
        Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(3))(result)
  }

  test("Interpreter - Expression.Let04") {
    // let x = false in if x then "abc" else "xyz"
    val input = Expression.Let(ident01, Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.IfThenElse(
        Expression.Var(ident01, Type.Bool),
        Expression.Lit(Literal.Str("abc", loc), Type.Str),
        Expression.Lit(Literal.Str("xyz", loc), Type.Str),
        Type.Str),
      Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("xyz"))(result)
  }

  test("Interpreter - Expression.Let05") {
    // let x = 14 - 3 in x + 2
    val input = Expression.Let(ident01,
      Expression.Binary(
        BinaryOperator.Minus,
        Expression.Lit(Literal.Int(14, loc), Type.Int),
        Expression.Lit(Literal.Int(3, loc), Type.Int),
        Type.Int),
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int),
        Expression.Lit(Literal.Int(2, loc), Type.Int),
        Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(13))(result)
  }

  test("Interpreter - Expression.Let06") {
    // let x = 14 - 3 in let y = 2 * 4 in x + y
    val input = Expression.Let(ident01,
      Expression.Binary(
        BinaryOperator.Minus,
        Expression.Lit(Literal.Int(14, loc), Type.Int),
        Expression.Lit(Literal.Int(3, loc), Type.Int),
        Type.Int),
      Expression.Let(ident02,
        Expression.Binary(
          BinaryOperator.Times,
          Expression.Lit(Literal.Int(2, loc), Type.Int),
          Expression.Lit(Literal.Int(4, loc), Type.Int),
          Type.Int),
        Expression.Binary(
          BinaryOperator.Plus,
          Expression.Var(ident01, Type.Int),
          Expression.Var(ident02, Type.Int),
          Type.Int),
        Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(19))(result)
  }

  test("Interpreter - Expression.Let07") {
    // let x = 1 in let y = x + 2 in let z = y + 3 in z
    val input = Expression.Let(ident01,
      Expression.Lit(Literal.Int(1, loc), Type.Int),
      Expression.Let(ident02,
        Expression.Binary(
          BinaryOperator.Plus,
          Expression.Var(ident01, Type.Int),
          Expression.Lit(Literal.Int(2, loc), Type.Int),
          Type.Int),
        Expression.Let(ident03,
          Expression.Binary(
            BinaryOperator.Plus,
            Expression.Var(ident02, Type.Int),
            Expression.Lit(Literal.Int(3, loc), Type.Int),
            Type.Int),
          Expression.Var(ident03, Type.Int),
          Type.Int),
        Type.Int),
      Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(6))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Match                                                     //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Pattern.Wildcard01") {
    // Unit match { case _ => 11 }
    val rules = List((Pattern.Wildcard(Type.Int), Expression.Lit(Literal.Int(11, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Unit(loc), Type.Unit), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(11))(result)
  }

  test("Interpreter - Pattern.Var01") {
    // 3 match { case x => x }
    val rules = List((Pattern.Var(ident01, Type.Int), Expression.Var(ident01, Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Int(3, loc), Type.Int), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(3))(result)
  }

  test("Interpreter - Pattern.Var02") {
    // 3 match { case x => x + 11 }
    val rules = List((Pattern.Var(ident01, Type.Int), Expression.Binary(
      BinaryOperator.Plus,
      Expression.Var(ident01, Type.Int),
      Expression.Lit(Literal.Int(11, loc), Type.Int),
      Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Int(3, loc), Type.Int), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(14))(result)
  }

  test("Interpreter - Pattern.Literal.Unit01") {
    // Unit match { case Unit => true }
    val rules = List((Pattern.Lit(Literal.Unit(loc), Type.Unit), Expression.Lit(Literal.Bool(true, loc), Type.Bool)))
    val input = Expression.Match(Expression.Lit(Literal.Unit(loc), Type.Unit), rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - Pattern.Literal.Bool01") {
    // true match { case true => 30 }
    val rules = List((Pattern.Lit(Literal.Bool(true, loc), Type.Bool), Expression.Lit(Literal.Int(30, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Bool(true, loc), Type.Bool), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(30))(result)
  }

  test("Interpreter - Pattern.Literal.Bool02") {
    // true match { case false => 0; case _ => 1 }
    val rules = List(
      (Pattern.Lit(Literal.Bool(false, loc), Type.Bool), Expression.Lit(Literal.Int(0, loc), Type.Int)),
      (Pattern.Wildcard(Type.Bool), Expression.Lit(Literal.Int(1, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Bool(true, loc), Type.Bool), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(1))(result)
  }

  test("Interpreter - Pattern.Literal.Int01") {
    // 87 match { case 87 => 1 }
    val rules = List((Pattern.Lit(Literal.Int(87, loc), Type.Int), Expression.Lit(Literal.Int(1, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Int(87, loc), Type.Int), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(1))(result)
  }

  test("Interpreter - Pattern.Literal.Int02") {
    // 87 match { case 86 => "foo"; case _ => "bar" }
    val rules = List(
      (Pattern.Lit(Literal.Int(86, loc), Type.Int), Expression.Lit(Literal.Str("foo", loc), Type.Str)),
      (Pattern.Wildcard(Type.Int), Expression.Lit(Literal.Str("bar", loc), Type.Str)))
    val input = Expression.Match(Expression.Lit(Literal.Int(87, loc), Type.Int), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("bar"))(result)
  }

  test("Interpreter - Pattern.Literal.Str01") {
    // "hello" match { case "hello" => "world" }
    val rules = List(
      (Pattern.Lit(Literal.Str("hello", loc), Type.Str), Expression.Lit(Literal.Str("world", loc), Type.Str)))
    val input = Expression.Match(Expression.Lit(Literal.Str("hello", loc), Type.Str), rules, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("world"))(result)
  }

  test("Interpreter - Pattern.Literal.Str02") {
    // "hello" match { case "bonjour" => 1; case "hola" => 2; case _ => 0 }
    val rules = List(
      (Pattern.Lit(Literal.Str("bonjour", loc), Type.Str), Expression.Lit(Literal.Int(1, loc), Type.Int)),
      (Pattern.Lit(Literal.Str("hola", loc), Type.Str), Expression.Lit(Literal.Int(2, loc), Type.Int)),
      (Pattern.Wildcard(Type.Str), Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(Literal.Str("hello", loc), Type.Str), rules, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - Pattern.Literal.Tag01") {
    // foo.bar.baz "hello world" match { case foo.bar.baz "hello world" => true }
    val name = Name.Resolved(List("foo", "bar"))
    val ident = Name.Ident("baz", loc)
    val tagTpe = Type.Tag(name, ident, Type.Str)
    val enumTpe = Type.Enum(Map("foo.bar.baz" -> tagTpe))
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, ident, Literal.Str("hello world", loc), enumTpe, loc), tagTpe),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, ident, Literal.Str("hello world", loc), enumTpe, loc), tagTpe),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - Pattern.Literal.Tag02") {
    // NameAndAge ("James", 42) match { case NameAndAge ("James", 40) => true; case _ => false }
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, ident,
        Literal.Tuple(List(Literal.Str("James", loc), Literal.Int(40, loc)), Type.Tuple(List(Type.Str, Type.Int)), loc),
        enumTpe, loc), tagTpe), Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      (Pattern.Wildcard(enumTpe), Expression.Lit(Literal.Bool(false, loc), Type.Bool)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, ident,
        Literal.Tuple(List(Literal.Str("James", loc), Literal.Int(42, loc)),
          Type.Tuple(List(Type.Str, Type.Int)), loc), enumTpe, loc), tagTpe),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Pattern.Literal.Tag03") {
    // ConstProp.Val 4 match { case ConstProp.Bot => true; case _ => false }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      (Pattern.Wildcard(enumTpe), Expression.Lit(Literal.Bool(false, loc), Type.Bool))
    )
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(4, loc), enumTpe, loc), tagTpeV),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Pattern.Literal.Tag04") {
    // ConstProp.Val 4 match { case ConstProp.Top => true; case _ => false }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      (Pattern.Wildcard(enumTpe), Expression.Lit(Literal.Bool(false, loc), Type.Bool))
    )
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(4, loc), enumTpe, loc), tagTpeV),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Pattern.Literal.Tag05") {
    // ConstProp.Val 4 match { case ConstProp.Val 4 => true; case _ => false }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identV, Literal.Int(4, loc), enumTpe, loc), tagTpeV),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      (Pattern.Wildcard(enumTpe), Expression.Lit(Literal.Bool(false, loc), Type.Bool))
    )
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(4, loc), enumTpe, loc), tagTpeV),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(true))(result)
  }

  test("Interpreter - Pattern.Literal.Tag06") {
    // ConstProp.Val 4 match { case ConstProp.Val 5 => true; case _ => false }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identV, Literal.Int(5, loc), enumTpe, loc), tagTpeV),
        Expression.Lit(Literal.Bool(true, loc), Type.Bool)),
      (Pattern.Wildcard(enumTpe), Expression.Lit(Literal.Bool(false, loc), Type.Bool))
    )
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(4, loc), enumTpe, loc), tagTpeV),
      rules, Type.Bool)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Bool(false))(result)
  }

  test("Interpreter - Pattern.Literal.Tuple01") {
    // ("hi", true) match { case ("hi", false) => 1; case _ => 2 }
    val rules = List(
      (Pattern.Lit(Literal.Tuple(List(Literal.Str("hi", loc), Literal.Bool(false, loc)),
        Type.Tuple(List(Type.Str, Type.Bool)), loc),
        Type.Tuple(List(Type.Str, Type.Bool))), Expression.Lit(Literal.Int(1, loc), Type.Int)),
      (Pattern.Wildcard(Type.Tuple(List(Type.Str, Type.Bool))), Expression.Lit(Literal.Int(2, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tuple(List(Literal.Str("hi", loc), Literal.Bool(true, loc)),
        Type.Tuple(List(Type.Str, Type.Bool)), loc), Type.Tuple(List(Type.Str, Type.Bool))),
      rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(2))(result)
  }

  test("Interpreter - Pattern.Literal.Tuple02") {
    // (4, (12, 8)) match { case (4, (12, 8)) => 24 }
    val rules = List((Pattern.Lit(
      Literal.Tuple(List(
        Literal.Int(4, loc),
        Literal.Tuple(List(Literal.Int(12, loc), Literal.Int(8, loc)),
          Type.Tuple(List(Type.Int, Type.Int)), loc)),
        Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int)))), loc),
      Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int))))),
      Expression.Lit(Literal.Int(24, loc), Type.Int)))
    val input = Expression.Match(Expression.Lit(
      Literal.Tuple(List(
        Literal.Int(4, loc),
        Literal.Tuple(List(Literal.Int(12, loc), Literal.Int(8, loc)),
          Type.Tuple(List(Type.Int, Type.Int)), loc)),
        Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int)))), loc),
      Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int))))),
    rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(24))(result)
  }

  test("Interpreter - Pattern.Tag01") {
    // NameAndAge ("James", 42) match { case NameAndAge (_, age) => age }
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val rules = List(
      (Pattern.Tag(name, ident,
        Pattern.Tuple(List(Pattern.Wildcard(Type.Str), Pattern.Var(ident01, Type.Int)),
          Type.Tuple(List(Type.Str, Type.Int))), tagTpe), Expression.Var(ident01, Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, ident,
        Literal.Tuple(List(Literal.Str("James", loc), Literal.Int(42, loc)),
          Type.Tuple(List(Type.Str, Type.Int)), loc), enumTpe, loc), tagTpe),
        rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(42))(result)
  }

  test("Interpreter - Pattern.Tag02") {
    // NameAndAge ("James", 42) match { case NameAndAge ("James", age) => age; case NameAndAge _ => 0 }
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val rules = List(
      (Pattern.Tag(name, ident,
        Pattern.Tuple(List(Pattern.Lit(Literal.Str("James", loc), Type.Str), Pattern.Var(ident01, Type.Int)),
          Type.Tuple(List(Type.Str, Type.Int))), tagTpe), Expression.Var(ident01, Type.Int)),
      (Pattern.Wildcard(Type.Str), Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, ident,
        Literal.Tuple(List(Literal.Str("James", loc), Literal.Int(42, loc)),
          Type.Tuple(List(Type.Str, Type.Int)), loc), enumTpe, loc), tagTpe),
      rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(42))(result)
  }

  test("Interpreter - Pattern.Tag03") {
    // NameAndAge ("John", 42) match { case NameAndAge ("James", age) => age; case NameAndAge _ => 0 }
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val rules = List(
      (Pattern.Tag(name, ident,
        Pattern.Tuple(List(Pattern.Lit(Literal.Str("James", loc), Type.Str), Pattern.Var(ident01, Type.Int)),
          Type.Tuple(List(Type.Str, Type.Int))), tagTpe), Expression.Var(ident01, Type.Int)),
      (Pattern.Wildcard(Type.Str), Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, ident,
        Literal.Tuple(List(Literal.Str("John", loc), Literal.Int(42, loc)),
          Type.Tuple(List(Type.Str, Type.Int)), loc), enumTpe, loc), tagTpe),
      rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - Pattern.Tag04") {
    // ConstProp.Top match {
    //   case ConstProp.Top => 0
    //   case ConstProp.Val v => v
    //   case ConstProp.Bot => 0
    // }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT),
        Expression.Lit(Literal.Int(0, loc), Type.Int)),
      (Pattern.Tag(name, identV, Pattern.Var(ident01, Type.Int), tagTpeV), Expression.Var(ident01, Type.Int)),
      (Pattern.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB),
        Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - Pattern.Tag05") {
    // ConstProp.Bot match {
    //   case ConstProp.Top => 0
    //   case ConstProp.Val v => v
    //   case ConstProp.Bot => 0
    // }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT),
        Expression.Lit(Literal.Int(0, loc), Type.Int)),
      (Pattern.Tag(name, identV, Pattern.Var(ident01, Type.Int), tagTpeV), Expression.Var(ident01, Type.Int)),
      (Pattern.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB),
        Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(0))(result)
  }

  test("Interpreter - Pattern.Tag06") {
    // ConstProp.Val 42 match {
    //   case ConstProp.Top => 0
    //   case ConstProp.Val v => v
    //   case ConstProp.Bot => 0
    // }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT),
        Expression.Lit(Literal.Int(0, loc), Type.Int)),
      (Pattern.Tag(name, identV, Pattern.Var(ident01, Type.Int), tagTpeV), Expression.Var(ident01, Type.Int)),
      (Pattern.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB),
        Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(42, loc), enumTpe, loc), tagTpeV), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(42))(result)
  }

  test("Interpreter - Pattern.Tag07") {
    // ConstProp.Val 100 match {
    //   case ConstProp.Top => 0
    //   case ConstProp.Val v => v
    //   case ConstProp.Bot => 0
    // }
    import ConstantPropTagDefs._
    val rules = List(
      (Pattern.Lit(Literal.Tag(name, identT, Literal.Unit(loc), enumTpe, loc), tagTpeT),
        Expression.Lit(Literal.Int(0, loc), Type.Int)),
      (Pattern.Tag(name, identV, Pattern.Var(ident01, Type.Int), tagTpeV), Expression.Var(ident01, Type.Int)),
      (Pattern.Lit(Literal.Tag(name, identB, Literal.Unit(loc), enumTpe, loc), tagTpeB),
        Expression.Lit(Literal.Int(0, loc), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tag(name, identV, Literal.Int(100, loc), enumTpe, loc), tagTpeV), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(100))(result)
  }

  test("Interpreter - Pattern.Tuple01") {
    // (5, 6) match { case (x, y) => x + y }
    val rules = List(
      (Pattern.Tuple(List(Pattern.Var(ident01, Type.Int), Pattern.Var(ident02, Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))),Expression.Binary(BinaryOperator.Plus,
        Expression.Var(ident01, Type.Int), Expression.Var(ident02, Type.Int), Type.Int)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tuple(List(Literal.Int(5, loc), Literal.Int(6, loc)),
        Type.Tuple(List(Type.Int, Type.Int)), loc), Type.Tuple(List(Type.Int, Type.Int))), rules, Type.Int)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Int(11))(result)
  }

  test("Interpreter - Pattern.Tuple02") {
    // (5, 6) match { case (5, 6) => "abc"; case (5, _) => "def"; case (_, _) => "ghi" }
    val rules = List(
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Lit(Literal.Int(6, loc), Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("abc", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("def", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Wildcard(Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("ghi", loc), Type.Str)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tuple(List(Literal.Int(5, loc), Literal.Int(6, loc)),
        Type.Tuple(List(Type.Int, Type.Int)), loc), Type.Tuple(List(Type.Int, Type.Int))), rules, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("abc"))(result)
  }

  test("Interpreter - Pattern.Tuple03") {
    // (5, 16) match { case (5, 6) => "abc"; case (5, _) => "def"; case (_, _) => "ghi" }
    val rules = List(
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Lit(Literal.Int(6, loc), Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("abc", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("def", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Wildcard(Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("ghi", loc), Type.Str)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tuple(List(Literal.Int(5, loc), Literal.Int(16, loc)),
        Type.Tuple(List(Type.Int, Type.Int)), loc), Type.Tuple(List(Type.Int, Type.Int))), rules, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("def"))(result)
  }

  test("Interpreter - Pattern.Tuple04") {
    // (15, 16) match { case (5, 6) => "abc"; case (5, _) => "def"; case (_, _) => "ghi" }
    val rules = List(
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Lit(Literal.Int(6, loc), Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("abc", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Lit(Literal.Int(5, loc), Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("def", loc), Type.Str)),
      (Pattern.Tuple(List(Pattern.Wildcard(Type.Int), Pattern.Wildcard(Type.Int)),
        Type.Tuple(List(Type.Int, Type.Int))), Expression.Lit(Literal.Str("ghi", loc), Type.Str)))
    val input = Expression.Match(
      Expression.Lit(Literal.Tuple(List(Literal.Int(15, loc), Literal.Int(16, loc)),
        Type.Tuple(List(Type.Int, Type.Int)), loc), Type.Tuple(List(Type.Int, Type.Int))), rules, Type.Str)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Str("ghi"))(result)
  }

  test("Interpreter - Expression.Match.Error01") {
    // 123 match { case 321 => Unit }
    val rules = List((Pattern.Lit(Literal.Int(321, loc), Type.Int), Expression.Lit(Literal.Unit(loc), Type.Unit)))
    val input = Expression.Match(Expression.Lit(Literal.Int(123, loc), Type.Int), rules, Type.Int)
    intercept[RuntimeException] {
      Interpreter.eval(input, root)
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Tuples and Tags                                           //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Tuple01") {
    val input = Expression.Tuple(List(
      Expression.Lit(Literal.Int(42, loc), Type.Int),
      Expression.Lit(Literal.Bool(false, loc), Type.Bool),
      Expression.Lit(Literal.Str("hi", loc), Type.Str)),
      Type.Tuple(List(Type.Int, Type.Bool, Type.Str)))
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tuple(List(Value.Int(42), Value.Bool(false), Value.Str("hi"))))(result)
  }

  test("Interpreter - Expression.Tuple02") {
    val input = Expression.Tuple(List(
      Expression.Lit(Literal.Int(4, loc), Type.Int),
      Expression.Tuple(List(Expression.Lit(Literal.Int(12, loc), Type.Int),
        Expression.Lit(Literal.Int(8, loc), Type.Int)), Type.Tuple(List(Type.Int, Type.Int)))),
      Type.Tuple(List(Type.Int, Type.Tuple(List(Type.Int, Type.Int)))))
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tuple(List(Value.Int(4), Value.Tuple(List(Value.Int(12), Value.Int(8))))))(result)
  }

  test("Interpreter - Expression.Tuple03") {
    val input = Expression.Tuple(List(
      // 40 + 2
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Lit(Literal.Int(40, loc), Type.Int),
        Expression.Lit(Literal.Int(2, loc), Type.Int),
        Type.Int),
      // !(-12 < 22)
      Expression.Unary(
        UnaryOperator.Not,
        Expression.Binary(
          BinaryOperator.Less,
          Expression.Lit(Literal.Int(-12, loc), Type.Int),
          Expression.Lit(Literal.Int(22, loc), Type.Int),
          Type.Bool),
        Type.Bool),
      // if (true) "hi" else "hello"
      Expression.IfThenElse(
        Expression.Lit(Literal.Bool(true, loc), Type.Bool),
        Expression.Lit(Literal.Str("hi", loc), Type.Str),
        Expression.Lit(Literal.Str("hello", loc), Type.Str),
        Type.Str)),
      Type.Tuple(List(Type.Int, Type.Bool, Type.Str)))
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tuple(List(Value.Int(42), Value.Bool(false), Value.Str("hi"))))(result)
  }

  test("Interpreter - Expression.Tag01") {
    val name = Name.Resolved(List("foo", "bar"))
    val ident = Name.Ident("baz", loc)
    val tagTpe = Type.Tag(name, ident, Type.Str)
    val enumTpe = Type.Enum(Map("foo.bar.baz" -> tagTpe))
    val input = Expression.Tag(name, ident,
      // if (!(4 != 4)) "hello world" else "asdfasdf"
      Expression.IfThenElse(
        Expression.Unary(
          UnaryOperator.Not,
          Expression.Binary(
            BinaryOperator.NotEqual,
            Expression.Lit(Literal.Int(4, loc), Type.Int),
            Expression.Lit(Literal.Int(4, loc), Type.Int),
            Type.Bool),
          Type.Bool),
        Expression.Lit(Literal.Str("hello world", loc), Type.Str),
        Expression.Lit(Literal.Str("asdfasdf", loc), Type.Str),
        Type.Str),
      enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "baz", Value.Str("hello world")))(result)
  }

  test("Interpreter - Expression.Tag02") {
    val name = Name.Resolved(List("Family"))
    val ident = Name.Ident("NameAndAge", loc)
    val tagTpe = Type.Tag(name, ident, Type.Tuple(List(Type.Str, Type.Int)))
    val enumTpe = Type.Enum(Map("Family.NameAndAge" -> tagTpe))
    val input = Expression.Tag(name, ident, Expression.Tuple(List(
      Expression.Lit(Literal.Str("James", loc), Type.Str),
      // 20 + 22
      Expression.Binary(
        BinaryOperator.Plus,
        Expression.Lit(Literal.Int(20, loc), Type.Int),
        Expression.Lit(Literal.Int(22, loc), Type.Int),
        Type.Int)),
      Type.Tuple(List(Type.Str, Type.Int))), enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "NameAndAge", Value.Tuple(List(Value.Str("James"), Value.Int(42)))))(result)
  }

  test("Interpreter - Expression.Tag03") {
    import ConstantPropTagDefs._
    val input = Expression.Tag(name, identB, Expression.Lit(Literal.Unit(loc), Type.Unit), enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Bot", Value.Unit))(result)
  }

  test("Interpreter - Expression.Tag04") {
    import ConstantPropTagDefs._
    val input = Expression.Tag(name, identT, Expression.Lit(Literal.Unit(loc), Type.Unit), enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Top", Value.Unit))(result)
  }

  test("Interpreter - Expression.Tag05") {
    import ConstantPropTagDefs._
    val input = Expression.Tag(name, identV,
      // 123 - 123
      Expression.Binary(
        BinaryOperator.Minus,
        Expression.Lit(Literal.Int(123, loc), Type.Int),
        Expression.Lit(Literal.Int(123, loc), Type.Int),
        Type.Int),
      enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(0)))(result)
  }

  test("Interpreter - Expression.Tag06") {
    import ConstantPropTagDefs._
    val input = Expression.Tag(name, identV,
      // -240
      Expression.Unary(
        UnaryOperator.UnaryMinus,
        Expression.Lit(Literal.Int(240, loc), Type.Int),
        Type.Int),
      enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(-240)))(result)
  }

  test("Interpreter - Expression.Tag07") {
    import ConstantPropTagDefs._
    val input = Expression.Tag(name, identV, Expression.Lit(Literal.Int(1241, loc), Type.Int), enumTpe)
    val result = Interpreter.eval(input, root)
    assertResult(Value.Tag(name, "Val", Value.Int(1241)))(result)
  }

  /////////////////////////////////////////////////////////////////////////////
  // Expressions - Error                                                     //
  /////////////////////////////////////////////////////////////////////////////

  test("Interpreter - Expression.Error01") {
    val input = Expression.Error(Type.Unit, loc)
    intercept[RuntimeException] {
      Interpreter.eval(input, root)
    }
  }

}
