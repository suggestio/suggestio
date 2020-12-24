package io.suggest.react

import io.suggest.common.html.HtmlConstants
import japgolly.scalajs.react.vdom.{HtmlAttrAndStyles, TagMod, TagOf, TopNode, VdomElement}
import japgolly.scalajs.react.{Callback, CallbackTo, ReactEvent}
import japgolly.scalajs.react.internal.OptionLike
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js
import scala.language.{higherKinds, implicitConversions}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.01.17 15:35
  * Description: Вспомогательная утиль для react-компонентов.
  */
object ReactCommonUtil {

  def stopPropagationCB(e: ReactEvent): Callback =
    e.stopPropagationCB
  def preventDefaultCB(e: ReactEvent): Callback =
    e.preventDefaultCB

  def cbFun1ToF[Arg, Res](fun: Arg => CallbackTo[Res]): (Arg) => Res = {
    fun
      .andThen( _.runNow() )
  }

  def cbFun0ToJsCb[Res](fun: () => CallbackTo[Res]): js.Function0[Res] = {
    {() =>
      fun().runNow
    }
  }

  /**
    * Приведение функции, возвращающей js Callback к js-функции-листенеру.
    * Это некая замена "==>" для pure-js-компонентов.
    * Без implicit, т.к. оно не работает в данном нетривиальном случае.
    *
    * @param fun Исходная фунция, возвращающая Callback.
    * @tparam Arg Тип исходного аргумента.
    * @tparam Res Тип возвращаемого из Callback'а значения.
    * @return js.Function1
    *
    * @see [[https://github.com/japgolly/scalajs-react/issues/210#issuecomment-149991727]]
    */
  def cbFun1ToJsCb[Arg, Res](fun: Arg => CallbackTo[Res]): js.Function1[Arg, Res] =
    cbFun1ToF(fun)

  def cbFun2ToJsCb[Arg1, Arg2, Res](fun: (Arg1, Arg2) => CallbackTo[Res]): js.Function2[Arg1, Arg2, Res] = {
    { (arg1, arg2) =>
      fun(arg1, arg2)
        .runNow()
    }
  }

  def cbFun3ToJsCb[A1,A2,A3,Res](fun: (A1,A2,A3) => CallbackTo[Res]): js.Function3[A1,A2,A3,Res] = {
    { (a1, a2, a3) =>
      fun(a1, a2, a3)
        .runNow()
    }
  }

  def cbFun4ToJsCb[A1,A2,A3,A4,Res](fun: (A1,A2,A3,A4) => CallbackTo[Res]): js.Function4[A1,A2,A3,A4,Res] = {
    { (a1, a2, a3, a4) =>
      fun(a1, a2, a3, a4)
        .runNow()
    }
  }


  val VdomNullElement: VdomElement =
    VdomElement(null)


  /** Выборочный рендер TagMod'а на основе результата выполнения условия. При компиляции превращается в if/else. */
  @inline
  def maybe(isRender: Boolean)(f: => TagMod): TagMod = {
    if (isRender)
      f
    else
      TagMod.empty
  }

  /** Выборочный рендер React-элемента на основе результата выполнения условия. При компиляции превращается в if/else. */
  @inline
  def maybeEl(isRender: Boolean)(f: => VdomElement): VdomElement = {
    if (isRender)
      f
    else
      VdomNullElement
  }

  @inline
  def maybeNode(isRender: Boolean)(f: => VdomNode): VdomNode = {
    if (isRender)
      f
    else
      EmptyVdom
  }

  /** Для прямой передачи сырого ref-а сквозь sjs vdom прямо в react.js, используем этот vdom-аттрибут.
    * @see Решение взято из [[https://github.com/japgolly/scalajs-react/issues/500#issuecomment-426559951]]
    */
  lazy val genericRefAttr = VdomAttr( VdomAttr.Ref.attrName )


  /** Все неявности складируются сюда. */
  object Implicits {

    implicit class HtmlAttrsOpsExt(val haas: HtmlAttrAndStyles ) extends AnyVal {
      /** Доступ к vdom-атрибуту ref generic, не проверяющий тип ref'а. */
      def genericRef = genericRefAttr
    }


    implicit class TagOfOpsExt[+N <: TopNode]( val tagOf: TagOf[N] ) extends AnyVal {
      def withGenericRef(ref: js.Any) = {
        tagOf(
          ^.genericRef := ref
        )
      }
    }


    /** Дополнительное API для react-рендера Option'ов. */
    implicit class VdomElOptionExt[O[_], A](val o: O[A])(implicit O: OptionLike[O]) {

      /**
       * Рендер Option[A] в VdomElement.
       * Это чтобы не мудрить везде с .orNull и прочими EmptyVdom, и заодно решить проблему с diode connect/wrap.
       */
      def whenDefinedEl(implicit f: A => VdomElement): VdomElement = {
        O.fold(o, VdomNullElement)(f)
      }

      /** Рендер Option[A] в VdomNode. */
      def whenDefinedNode(implicit f: A => VdomNode): VdomNode = {
        O.fold(o, VdomArray.empty(): VdomNode)(f)
      }

    }


    /** Дополнительное API для react-рендера TagMod'ов. */
    implicit class VdomTagModExt(val tm: TagMod) extends AnyVal {

      /** Форсированное приведение TagMod'а к vdom-элементу.
        * Если текущий TagMod уже является vdom-элементом, то его и вернуть.
        * Иначе, завернуть в указанный тег.
        *
        * @param maybeWrapInto Функция, возвращающая тег-заворачиватель.
        * @return Элемент vdom.
        */
      def forceVdomElement[N <: TopNode](maybeWrapInto: => TagOf[N]): VdomElement = {
        tm match {
          case ve: VdomElement  => ve
          case null             => VdomNullElement
          case _                => maybeWrapInto(tm)
        }
      }

    }


    /** Доп.утиль для строк в контексте рендера оных в реакте. */
    implicit class ReactStringsExtOps(val str: String) extends AnyVal {

      /** Изредка бывает, что нужно заменить пробелы в строке на NBSP.
        * Например, когда рендером MPrice занимается сервер из-за нереализованности этого на клиенте.
        */
      def spacingToVdomNbspStrings: VdomArray = {
        val nbsp = HtmlConstants.NBSP_STR
        (for {
          part <- str.split("\\s+").iterator
          delimited <- (nbsp :: part :: Nil)
        } yield {
          delimited: VdomNode
        })
          .to( LazyList )
          .tail
          .toVdomArray
      }

    }

  }


  /** Применить HOC-функции (high-order component) к компоненту, вернув обновлённый абстрактный root-компонент.
    *
    * @param component root-компонент.
    * @param hocs HOC-функции, привидённые к scala-функциям (для удобства отработки нестандартных ситуаций).
    * @return Новый root-компонент.
    */
  def applyHocs(component: js.Object)(hocs: Function1[js.Object, js.Object]*): js.Object = {
    hocs
      .foldLeft(component) { (comp0, hocF) =>
        hocF(comp0)
      }
  }

}
