package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.{MLknNode, MLknOpKey, MLknOpValue}
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.spa.DiodeUtil
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
  def tfInfoWide  = GenLens[MNodeState](_.tfInfoWide)
  def optionMods  = GenLens[MNodeState](_.optionMods)
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
                       tfInfoWide         : Boolean                           = false,
                       optionMods         : Map[MLknOpKey, Pot[MLknOpValue]]  = Map.empty,
                       beacon             : Option[MNodeBeaconState]          = None,
                     ) {

  def optionBoolPot(key: MLknOpKey): Pot[Boolean] = {
    optionMods
      .getOrElse( key, Pot.empty )
      .orElse[MLknOpValue] {
        infoPot
          .flatMap { info =>
            Pot.fromOption( info.options.get( key ) )
          }
      }
      .flatMap { opVal =>
        opVal
          .bool
          // Гарантировать ссылочную целостность для стабильных инстансов Pot(true) и Pot(false):
          .fold( Pot.empty[Boolean] )( DiodeUtil.Bool.apply )
      }
  }

  def beaconUidOpt = beacon
    .flatMap( _.data.detect.signal.beaconUid )

  def nodeId: Option[String] = {
    infoPot
      .toOption
      .map(_.id)
      .orElse( beaconUidOpt )
  }

}
