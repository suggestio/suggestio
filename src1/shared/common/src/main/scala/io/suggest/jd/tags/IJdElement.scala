package io.suggest.jd.tags

import io.suggest.color.MColorData
import io.suggest.jd.MJdEdgeId

import scala.language.higherKinds
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.10.17 21:37
  * Description: Jd-элемент: Общий интерфейс для jd-тегов, qd-тегов и qd-операций.
  */

trait IJdElement {

  /** Вернуть инстанс Scalaz Tree.
    *
    * Запилен для экспериментов и набора опыта,
    * TODO в более дальних планах было отделить древовидную структуру
    * от конкретных классов: IDocTag и QdOp. И удалить этот метод за ненадобностью.
    *
    * @return Хорошее годное деревце.
    */
  def toScalazTree: Tree[IJdElement]


  /** Глубинный элементарный маппинг с помощью API внутри этого метода.
    * Не следует внутри функции выходить за пределы API [[IJdElement]], и всё будет ок.
    * На деле, это следовало бы реализовать как-то через
    * scalaz.Tree[IJdElement] + scalaz.Functor[Tree](template)(f: IJdElement => IJdElement).
    * но для этого нужно разломать всё исходное дерево.
    * $1 -- это очень исходный элемент.
    * $2 -- это текущий инстанс, который надо модифицировать.
    */
  def deepElMap(f: (IJdElement) => IJdElement): IJdElement

  def bgImgEdgeId: Option[MJdEdgeId]

  /** Выставить текущему элементу указанный цвет фона. */
  def setBgColor(bgColor: Option[MColorData]): IJdElement

}

