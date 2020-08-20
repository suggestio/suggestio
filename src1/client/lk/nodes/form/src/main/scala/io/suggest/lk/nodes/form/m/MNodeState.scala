package io.suggest.lk.nodes.form.m

import diode.data.Pot
import io.suggest.adv.rcvr.RcvrKey
import io.suggest.lk.nodes.{MLknAdv, MLknNode}
import io.suggest.spa.DiodeUtil
import japgolly.univeq._
import io.suggest.ueq.JsUnivEqUtil._
import monocle.macros.GenLens
import scalaz.TreeLoc

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


  implicit class MnsLocExt(private val loc: TreeLoc[MNodeState]) extends AnyVal {

    /** Сборка цепочки id узлов от корня до указанного узла. */
    def rcvrKey: RcvrKey = {
      loc
        .path
        .map(_.info.id)
        .reverse
        .toList
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

  require( info.nonEmpty )

  def info = infoPot.get

  /** Является ли текущее состояние узла нормальным и обычным?
    *
    * @return true, значит можно отрабатывать клики по заголовку в нормальном режиме.
    *         false -- происходит какое-то действо, например переименование узла.
    */
  def isNormal = !advIsPending

  def advIsPending = adv.exists(_.newIsEnabledPot.isPending)

  private def _boolPot(from: MNodeAdvState => Pot[Boolean], fallBack: MLknAdv => Boolean): Pot[Boolean] = {
    adv
      .map(from)
      .orElse(
        for (adv <- info.adv) yield
          DiodeUtil.Bool( fallBack(adv) )
      )
      .getOrElse( Pot.empty[Boolean] )
  }

  // TODO Надо унифицировать этот однотипный зоопарк сущностей в коллекцию или ассоц.массив.
  def advHasAdvPot = _boolPot(_.newIsEnabledPot, _.hasAdv)
  def advAlwaysOpenedPot = _boolPot(_.isShowOpenedPot, _.advShowOpened)
  def advAlwaysOutlinedPot = _boolPot(_.alwaysOutlinedPot, _.alwaysOutlined)

}
