package models.msc

import io.suggest.model.n2.node.MNode
import play.twirl.api.Html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.09.16 18:27
  * Description: Модель синхронных/внешних аргументов sc index.
  *
  * Это и не аргументы вызова, и не аргументы рендера, а некое дополнение к аргументам рендера.
  * Например, это отрендеренные данные, приходящие из
  *
  * Исторически, они неуместно жили в qs-модели [[MScIndexArgs]] (ScReqArgs), вообще не являясь qs-параметрами.
  * Этот архитектурный дефект осложнял рефакторинг на новую геолокацию.
  */

trait IScIndexSyncArgs extends SyncRenderInfo {

  /** Отрендеренные элементы плитки для рендера прямо в indexTpl.
    * Передаются при внутренних рендерах, вне обыденных HTTP-запросов и прочего. */
  def inlineTiles         : Seq[IRenderedAdBlock]

  /** Отрендеренная focused-выдача. */
  def focusedContent      : Option[Html]

  /** Отрендеренный список нод на nav-панели. */
  def inlineNodesList     : Option[Html]

  /** nav-панель, geo nodes list: текущая нода по списку нод, если известна.
    * TODO Удалить потом, следом за nav-панелью. */
  def adnNodeCurrentGeo   : Option[MNode]


  override def toString: String = {
    val sb = new StringBuilder(32, getClass.getSimpleName)
      .append('(')

    val _inlTls = inlineTiles
    if (_inlTls.nonEmpty)
      sb.append( _inlTls.size ).append("tiles")

    if (focusedContent.nonEmpty)
      sb.append("+foc.content")

    if (inlineNodesList.nonEmpty)
      sb.append("+inlineNodeList")

    for (mnode <- adnNodeCurrentGeo)
      sb.append(s"+geoNode[").append(mnode.idOrNull).append(']')

    sb.append(')')
      .toString()
  }

}


/** Пустая реализация модели [[IScIndexSyncArgs]]. */
trait MScIndexSyncArgs extends IScIndexSyncArgs with SyncRenderInfoDflt {
  override def inlineTiles: Seq[IRenderedAdBlock] = Nil
  override def focusedContent: Option[Html] = None
  override def inlineNodesList: Option[Html] = None
  override def adnNodeCurrentGeo: Option[MNode] = None
}
class MScIndexSyncArgsImpl extends MScIndexSyncArgs

object MScIndexSyncArgs {
  val empty = new MScIndexSyncArgsImpl
}


/** wrap-реализация модели [[IScIndexSyncArgs]]. */
trait MScIndexSyncArgsWrap extends IScIndexSyncArgs {

  def _syncArgsUnderlying: IScIndexSyncArgs

  override def inlineTiles        = _syncArgsUnderlying.inlineTiles
  override def focusedContent     = _syncArgsUnderlying.focusedContent
  override def inlineNodesList    = _syncArgsUnderlying.inlineNodesList
  override def adnNodeCurrentGeo  = _syncArgsUnderlying.adnNodeCurrentGeo
  override def jsStateOpt         = _syncArgsUnderlying.jsStateOpt

}

