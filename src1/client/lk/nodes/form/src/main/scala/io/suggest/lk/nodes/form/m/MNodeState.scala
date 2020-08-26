package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.{MLknAdv, MLknNode}
import io.suggest.spa.DiodeUtil
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens
import scalaz.{Tree, TreeLoc}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 16:36
  * Description: Модель рантаймового состояния одного узла в списке узлов дерева.
  */

object MNodeState {

  @inline implicit def univEq: UnivEq[MNodeState] = UnivEq.derive

  def infoPot     = GenLens[MNodeState](_.infoPot)
  def isEnableUpd = GenLens[MNodeState](_.isEnabledUpd)
  def tfInfoWide  = GenLens[MNodeState](_.tfInfoWide)
  def adv         = GenLens[MNodeState](_.adv)


  /** Обработать под-дерево узлов, присланное сервером. */
  def processTree(serverTree: Tree[MLknNode]): Tree[MNodeState] = {
    for (lkNode <- serverTree) yield {
      MNodeState(
        infoPot = Pot.empty.ready( lkNode ),
      )
    }
  }


  implicit class MnsLocExt(private val loc: TreeLoc[MNodeState]) extends AnyVal {

    /** Сборка цепочки id узлов от корня до указанного узла. */
    def rcvrKey: RcvrKey = {
      (for {
        mns <- loc.path.iterator
        info <- mns.infoPot.toOption
      } yield {
        info.id
      })
        .toList
        .reverse
    }

  }

}


/** Класс модели рантаймового состояния одного узла в списке узлов.
  *
  * @param infoPot Данные по узлу, присланные сервером.
  *                Pot нужен, т.к. существует недетализованное содержимое узла, когда нет части данных.
  *                По идее, Pot.empty тут не бывает.
  */
case class MNodeState(
                       infoPot            : Pot[MLknNode],
                       isEnabledUpd       : Option[MNodeEnabledUpdateState]   = None,
                       tfInfoWide         : Boolean                           = false,
                       adv                : Option[MNodeAdvState]             = None,
                     ) {

  def advIsPending = adv.exists(_.newIsEnabledPot.isPending)

  private def _boolPot(from: MNodeAdvState => Pot[Boolean], fallBack: MLknAdv => Boolean): Pot[Boolean] = {
    adv
      .map(from)
      .orElse(
        for {
          info <- infoPot.toOption
          adv <- info.adv
        } yield {
          DiodeUtil.Bool( fallBack(adv) )
        }
      )
      .getOrElse( Pot.empty[Boolean] )
  }

  // TODO Надо унифицировать этот однотипный зоопарк сущностей в коллекцию или ассоц.массив.
  def advHasAdvPot = _boolPot(_.newIsEnabledPot, _.hasAdv)
  def advAlwaysOpenedPot = _boolPot(_.isShowOpenedPot, _.advShowOpened)
  def advAlwaysOutlinedPot = _boolPot(_.alwaysOutlinedPot, _.alwaysOutlined)

}
