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
  def beacon      = GenLens[MNodeState](_.beacon)


  /** Обработать под-дерево нормальных N2-узлов, присланное сервером
    * (или наподобии того, что сервер должен бы прислать).
    * @param tree Дерево или поддерево.
    */
  def respTreeToIdsTree(tree: Tree[MLknNode]): Tree[String] = {
    tree.map(_.id)
  }

  /** Рендер в nodes map. */
  def fromRespNode(lknNode: MLknNode): MNodeState = {
    MNodeState(
      infoPot = Pot.empty.ready( lknNode ),
      role    = MTreeRoles.Normal,
    )
  }


  /** Собрать корневой элемент. */
  def mkRootNode = MNodeState(
    infoPot = Pot.empty,
    role    = MTreeRoles.Root,
  )


  implicit class MnsLocExt(private val nodesMap: Map[String, MNodeState]) extends AnyVal {

    def mnsPath(loc: TreeLoc[String]) = loc
      .path
      .iterator
      .flatMap( nodesMap.get )

  }


  implicit class MnsIterExt( private val mnss: IterableOnce[MNodeState] ) extends AnyVal {
    /** .mnsPath().rcvrKey */
    def rcvrKey: RcvrKey = {
      mnss
        .iterator
        .takeWhile( _.role.isRealNode )
        .flatMap(_.nodeId)
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
  * @param beacon Данные по сигналу от маячка поблизости.
  */
case class MNodeState(
                       infoPot            : Pot[MLknNode]                     = Pot.empty,
                       role               : MTreeRole,
                       isEnabledUpd       : Option[MNodeEnabledUpdateState]   = None,
                       tfInfoWide         : Boolean                           = false,
                       adv                : Option[MNodeAdvState]             = None,
                       beacon             : Option[MNodeBeaconState]          = None,
                     ) {

  def advIsPending = adv.exists(_.newIsEnabledPot.isPending)

  private def _boolPot(
                        from: MNodeAdvState => Pot[Boolean],
                        fallBack: MLknAdv => Boolean,
                      ): Pot[Boolean] = {
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

  def beaconUidOpt = beacon
    .flatMap(_.data.detect.signal.beaconUid)

  def nodeId: Option[String] = {
    infoPot
      .toOption
      .map(_.id)
      .orElse( beaconUidOpt )
  }

}
