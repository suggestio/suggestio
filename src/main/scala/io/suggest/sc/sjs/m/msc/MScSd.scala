package io.suggest.sc.sjs.m.msc

import io.suggest.sc.sjs.m.mfoc.MFocSd
import io.suggest.sc.sjs.m.mgrid.{MGridData, MGridState}
import io.suggest.sc.sjs.m.mnav.MNavState
import io.suggest.sc.sjs.m.msearch.MSearchSd
import io.suggest.sc.sjs.util.logs.ScSjsLogger
import io.suggest.sjs.common.msg.WarnMsgs

import scala.scalajs.js.URIUtils

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.06.15 17:33
 * Description: Модель состояния конечного автомата интерфейса выдачи.
 */

object MScSd extends ScSjsLogger {

  import io.suggest.sc.ScConstants.ScJsState._

  type AccEl_t  = (String, Any)
  type Acc_t    = List[AccEl_t]

  /** Сериализация значимых частей состояния в список ключ-значение. */
  def toUrlHashAcc(sd0: MScSd): Acc_t = {
    var acc: List[(String, Any)] = Nil

    // Пока пишем generation, но наверное это лучше отключить, чтобы в режиме iOS webapp не было повторов.
    acc ::= GENERATION_FN -> MGen.serialize(sd0.common.generation)

    // Отработка состояния левой панели.
    val npo = sd0.nav.panelOpened
    if (npo) {
      acc ::= GEO_SCR_OPENED_FN -> npo
    }

    // Отрабатываем состояние правой панели.
    val spo = sd0.search.opened
    if (spo) {
      acc = CAT_SCR_OPENED_FN -> spo ::
        SEARCH_TAB_FN -> sd0.search.currTab.strId ::
        acc
    }

    // Отработать focused-выдачу, если она активна.
    for (focSd <- sd0.focused) {
      acc ::= FADS_CURRENT_AD_ID_FN -> focSd.current.madId
      // Закинуть producerId foc-выдачи
      for (producerId <- focSd.producerId) {
        acc ::= PRODUCER_ADN_ID_FN -> producerId
      }
    }

    // Отработать id текущего узла.
    for (nodeId <- sd0.common.adnIdOpt) {
      acc ::= ADN_ID_FN -> nodeId
    }

    acc
  }


  /** Сериализация списк ключ-значение в строку для URL.
    *
    * @param acc Выхлоп toUrlHashAcc().
    * @return Строка, пригодная для записи в URL.
    */
  def acc2Qs(acc: TraversableOnce[AccEl_t]): String = {
    acc.toIterator
      .map { kv =>
        kv.productIterator
          .map { s =>
            URIUtils.encodeURIComponent(s.toString)
          }
          .mkString("=")
      }
      .mkString("&")
  }

  /** Простая сериализация инстанса модели в строку. */
  def toQsStr(sd: MScSd): String = {
    acc2Qs(
      toUrlHashAcc(sd)
    )
  }


  /** Парсинг Qs в строковые токены..
    *
    * @param qs Выхлоп acc2Qs().
    * @return Распарсенные токены QS в прямом порядке.
    */
  def parseFromQs(qs: String): Map[String, String] = {
    qs.split('&')
      .iterator
      .flatMap { kvStr =>
        if (kvStr.isEmpty) {
          Nil
        } else {
          kvStr.split('=') match {
            case arr if arr.length == 2 =>
              val arr2 = arr.iterator
                .map(URIUtils.decodeURIComponent)
              val k2 = arr2.next()
              val v2 = arr2.next()
              List(k2 -> v2)

            case other =>
              warn( WarnMsgs.MSC_STATE_URL_HASH_UNKNOWN_TOKEN + " " + other )
              Iterator.empty
          }
        }
      }
      .toMap
  }

}


/** Интерфейс контейнера данный полного состояния выдачи. */
trait IScSd {

  /** Контейнер для общих полей resizeOpt, screen, browser и прочих. */
  def common      : MScCommon

  /** Контейнер данных состояния плитки карточек. */
  def grid        : MGridData

  /** Контейнер данных состояния поиска и поисковой панели. */
  def search      : MSearchSd

  /** Контейнер данных состояния панели навигации и навигации в целом. */
  def nav         : MNavState

  /** Контейнер для данных focused-выдачи.
    * None значит, что focused-выдача отключена. */
  def focused     : Option[MFocSd]

  /** @return true если открыта какая-то боковая панель.
    *         false -- ни одной панели не открыто. */
  def isAnySidePanelOpened: Boolean = {
    nav.panelOpened || search.opened
  }

  /**
    * Проверка, изменился ли "корень" данной выдачи
    * (корень -- id узла или текущая геолокация, от которой всё пляшет)?
    */
  def isScRootDiffers(sd2: IScSd): Boolean = {
    common.adnIdOpt != sd2.common.adnIdOpt
  }

  /**
    * Проверка на то, отличается ли выдача в целом исходя из её базовых параметров.
    * Визуально и по содержимому.
    * screen не проверяется, т.к. оно отрабатывается через viewportChanged().
    */
  def isScDiffers(sd2: IScSd): Boolean = {
    isScRootDiffers(sd2) ||
      common.generation != sd2.common.generation
  }

}


/** Реализация immutable-контейнера для передачи данных Sc FSM между состояниями. */
case class MScSd(
  override val common       : MScCommon,
  override val grid         : MGridData             = MGridData(),
  override val search       : MSearchSd             = MSearchSd(),
  override val nav          : MNavState             = MNavState(),
  override val focused      : Option[MFocSd]        = None
)
  extends IScSd
{

  def withCommon(common2: MScCommon) = copy(common = common2)

  def withGrid(grid2: MGridData) = copy(grid = grid2)

  /**
   * При переключении узла надо резко менять и чистить состояние. Тут логика этого обновления.
   *
   * @param adnIdOpt2 Новый id текущего узла-ресивера.
   * @return Новый экземпляр состояния.
   */
  def withNodeSwitch(adnIdOpt2: Option[String]): MScSd = {
    copy(
      common = common.copy(
        adnIdOpt  = adnIdOpt2,
        geoLocOpt = None,
        tagOpt    = None
      ),
      nav       = MNavState(),
      search    = MSearchSd(),
      grid      = MGridData(
        state = MGridState(
          adsPerLoad = grid.state.adsPerLoad
        )
      ),
      focused   = None
    )
  }

  def notFocused = copy(focused = None)

}
