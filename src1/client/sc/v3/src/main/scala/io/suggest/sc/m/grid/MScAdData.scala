package io.suggest.sc.m.grid

import diode.data.Pot
import io.suggest.common.coll.Lists
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.log.Log
import io.suggest.primo.id.OptStrId
import io.suggest.scalaz.ScalazUtil.Implicits.OptionExt
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.{Cord, EphemeralStream, Show, Tree, TreeLoc}

import scala.annotation.tailrec

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 19:07
  * Description: Модель рантаймовой инфы по одному блоку рекламной карточки в выдаче.
  * Модель собирается на базе созвучной MJdAdData, более простой и абстрактной по сравнению с этой.
  */
object MScAdData extends Log {

  /** Пустой инстанс [[MScAdData]], годен только для rootLabel в дереве grid ads. */
  def empty = apply(
    data          = Pot.empty,
    gridItems     = Nil,
    partialItems  = false,
  )

  @inline implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  def data = GenLens[MScAdData](_.data)
  def gridItems = GenLens[MScAdData](_.gridItems)
  def partialItems = GenLens[MScAdData](_.partialItems)


  implicit final class ScAdDataOpsExt( private val scAd: MScAdData ) extends AnyVal {

    def nodeId: Option[String] = {
      scAd
        .data
        .toOption
        .flatMap( _.doc.tagId.nodeId )
    }

    def canEdit: Boolean = {
      scAd
        .data
        .exists { foc =>
          (foc.info.canEditOpt contains[Boolean] true) &&
          // Запрещаем 404-карточки считать за "свои".
          !foc.info.isMad404
        }
    }

    /** Вернуть первый gridKey, который можно задействовать для обозначения всей карточки в текущей ветке. */
    def firstGridKey: Option[GridAdKey_t] = {
      scAd.gridItems
        .headOption
        .map(_.gridKey)
    }

    def gridItemWithKey(gridKey: GridAdKey_t): Option[MGridItem] = {
      scAd.gridItems.find(_.gridKey ==* gridKey)
    }

  }


  implicit final class GridPtrTreeLocExt( private val adsTreeLoc: TreeLoc[MScAdData] ) extends AnyVal {

    /** Перебор всех карточек по grid key. Учитывая, что ключ может дублировать на подуровнях при распиливании
      * карточки или ином клонировании её кусков, этот метод не слишком надёжен. Лучше использовать gridKeyPath-методы.
      * @param gridKey Ключ элемента плитки.
      * @return Первое найденное местоположение ключа.
      *         На под-уровнях этот ключ может повторятся при клонировании частей карточки.
      */
    def findByGridKey(gridKey: GridAdKey_t): Option[TreeLoc[MScAdData]] = {
      adsTreeLoc
        .find( _.getLabel.gridItemWithKey(gridKey).nonEmpty )
    }


    /** Аналог NodePath, но вместо позиций используется [[ScAdDataOpsExt.firstGridKey]].
      * Позволяет иметь стабильные id в плитке, которые не ломаются при любых изменениях в плитке.
      * Традиционно, путь до корня является [].
      */
    def ephemeralGridKeyPath: EphemeralStream[GridAdKey_t] = {
      (
        adsTreeLoc.getLabel ##::
        adsTreeLoc
          .parents
          .map(_._2)
      )
        .map( _.firstGridKey )
        // Вернуть в прямом порядке (от корня вглубь):
        .reverse
        // Гарантированно отбросить root-элемент (который обычно None, но у нас тут гарантии отброса независимо от значения):
        .tailOption
        // Сплющить полученный список опциональных grid key:
        .toEphemeralStream
        .flatMap( identity )
        .flatMap( _.toEphemeralStream )
    }

    /** List: Уходить от EphemeralStream, чтобы не держать в памяти TreeLoc вместе со всеми зависимостями. */
    def gridKeyPath: List[GridAdKey_t] =
      ephemeralGridKeyPath.toList

    /** Поиск карточки в дереве с помощью пути до неё.
      * Конкретный gridItem внутри карточки ищется уже при необходимости отдельно (не здесь).
      *
      * @param pathIter Итератор из выхлопов gridKeyPath() или ephemeralGridKeyPath()
      * @return Опциональный TreeLoc для найденного узла grid-дерева.
      */
    def findByGridKeyPath(pathIter: IterableOnce[GridAdKey_t]): Option[TreeLoc[MScAdData]] =
      _pathToNodeLoc( adsTreeLoc, pathIter.iterator )


    /** Отбросить лишние дочерние, кроме локации в дереве по указанному пути.
      *
      * @param keepPath Сохранить данные по указанному пути.
      * @return Локация в дереве.
      */
    def dropChildrenUntilPath(keepPath: List[GridAdKey_t]): TreeLoc[MScAdData] = {
      val keepPathLen = keepPath.length

      /** Идём всю плитку по нижним уровням, отбрасывая ненужные под-уровни, сверяясь с keepPath-ориентиром.
        *
        * @param parentNodePathRev Путь до родительского узла (развёрнутый)
        * @param loc Текущая локация в дереве.
        * @param canDropChildren Можно ли на текущем шаге спиливать нижний уровень?
        * @param isRootNode Это корневой элемент?
        * @param canWalkDown Разрешено ли идти вниз по дереву?
        * @return Обновлённая локация для сборки обновлённого дерева.
        */
      @tailrec
      def __walker(parentNodePathRev: List[GridAdKey_t],
                   loc: TreeLoc[MScAdData],
                   canDropChildren: Boolean,
                   isRootNode: Boolean = false,
                   canWalkDown: Boolean = true,
                  ): TreeLoc[MScAdData] = {
        // В первом if-else могут использоваться эти переменные в условии и в самом теле:
        lazy val (currNodePathRev, currNodePath, commonKeepPrefixLen, isOnTheTargetPathNow) = {
          // Текущий путь до узла:
          val _currNodePathRev = loc.getLabel
            .firstGridKey
            .filter( _ => !isRootNode )
            .fold( parentNodePathRev )( _ :: parentNodePathRev )
          val _currNodePath = _currNodePathRev.reverse
          val _commonKeepPrefix = Lists.largestCommonPrefix( _currNodePath, keepPath )
          val _commonKeepPrefixLen = _commonKeepPrefix.length
          val _isOnTheTargetPathNow = (_commonKeepPrefixLen ==* keepPathLen)

          (_currNodePathRev, _currNodePath, _commonKeepPrefixLen, _isOnTheTargetPathNow)
        }

        // Есть дочерние элементы и возможен спуск на уровень ниже. Нужно решить, дропаем ли children на текущем уровне?
        if (
          canWalkDown &&
          loc.hasChildren && {
            isOnTheTargetPathNow ||                          // Мы на обоначенной цели
            (commonKeepPrefixLen ==* currNodePath.length)    // Либо, идём по пути к цели, на каком-то промежуточном шаге. Продолжаем погружение вглубь.
          }
        ) {
          // Этот узел оставляем вместе с под-элементами. Но под-под-элементы надо зачищать:
          val firstChildLoc = loc.firstChild.get
          __walker( currNodePathRev, firstChildLoc, canWalkDown = !isOnTheTargetPathNow, canDropChildren = true )

        } else if ( canDropChildren && loc.hasChildren ) {
          // Мы или достигли цели и гуляем по подуровням цели, или какой-либо не-keepPath-элемент. Дропаем все элементы ниже:
          val loc2 = loc.setTree(
            Tree.Leaf( loc.getLabel )
          )
          __walker( parentNodePathRev, loc2, canWalkDown = false, canDropChildren = false )

        } else if (!loc.rights.isEmpty) {
          // Есть элемент справа, хотя нет children'ов. Один шаг вправо...
          val rightLoc = loc.right.get
          __walker( parentNodePathRev, rightLoc, canWalkDown = true, canDropChildren = true )

        } else if (!loc.parents.isEmpty) {
          // Нет ни справа, ни внизу. Подняться на шаг вверх:
          val parentLoc = loc.parent.get
          val isRootNode2 = parentNodePathRev.isEmpty
          val parentPathRev2 = if (isRootNode2) parentNodePathRev else parentNodePathRev.tail
          __walker( parentPathRev2, parentLoc, canWalkDown = false, canDropChildren = false, isRootNode = isRootNode2 )

        } else {
          // Нет родительских элементов и !canWalkDown. Вы прошли дерево и вернулись на самый верх. Вернуть текущую локацию в дереве.
          loc
        }
      }

      __walker(
        parentNodePathRev = Nil,
        loc               = adsTreeLoc,
        isRootNode        = true,
        canWalkDown       = true,
        canDropChildren   = false,
      )
    }

  }


  /** Рекурсивное путешествие по дереву до карточки с помощью указанного пути. */
  private def _pathToNodeLoc(loc: TreeLoc[MScAdData], iter: Iterator[GridAdKey_t]): Option[TreeLoc[MScAdData]] = {
    iter
      .nextOption()
      .fold[Option[TreeLoc[MScAdData]]] {
        Some( loc )
      } { gridKey =>

        // Перебрать subForest в поисках указанного firstGridKey
        def __walkRight(chLocOpt: Option[TreeLoc[MScAdData]]): Option[TreeLoc[MScAdData]] = {
          // Обход вправо в поисках указанного gridKey
          chLocOpt.flatMap { chLoc =>
            if (chLoc.getLabel.gridItems.exists(_.gridKey ==* gridKey)) {
              // Найден необходимый элемент на текущем шаге.
              Some( chLoc )
            } else {
              __walkRight( chLoc.right )
            }
          }
        }

        __walkRight( loc.firstChild )
          .flatMap( _pathToNodeLoc(_, iter) )
      }
  }


  implicit def showScAdDataTreeItem: Show[MScAdData] = new Show[MScAdData] {
    override def show(f: MScAdData): Cord = {
      // Тут быстрый костыль:
      Cord( f.data.map(_.doc.tagId.toString).getOrElse("?") + ": " + f.gridItems.iterator.map(_.gridKey).mkString("[", " ", "]") )
    }
  }

}


/** Класс-контейнер данных по одному блоку плитки.
  * @param data Данные карточки с сервера.
  * @param gridItems Пронумерованные элементы плитки, которые будут отрендерены в плитке.
  * @param partialItems Описывает происхождение gridItems:
  *                     false - все блоки из текущей карточки (пускай даже только один main-блок)
  *                     true - это какой-то искусственный прозводный набор блоков, а полная карточка - в другом месте.
  */
final case class MScAdData(
                            data            : Pot[MJdDataJs],
                            gridItems       : Seq[MGridItem],
                            partialItems    : Boolean,
                          )
extends OptStrId
{

  override def id = this.nodeId

}
