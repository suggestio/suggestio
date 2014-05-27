package controllers

import _root_.util.qsb.AdSearch
import util.PlayMacroLogsImpl
import util.acl._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.SiowebEsUtil.client
import scala.concurrent.Future
import io.suggest.ym.model.common.EMAdnMMetadataStatic.META_FLOOR_ESFN
import io.suggest.model.EsModel
import views.html.market.lk.adn._, _node._
import io.suggest.ym.model.MAdnNode
import play.api.data.Form
import play.api.data.Forms._
import util.FormUtil._
import play.api.libs.json._
import io.suggest.ym.model.common.AdShowLevels

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.04.14 11:18
 * Description: Унифицированные части личного кабинета.
 */
object MarketLkAdn extends SioController with PlayMacroLogsImpl {

  import LOGGER._

  /**
   * Отрендерить страницу ЛК абстрактного узла рекламной сети.
   * @param adnId id узла.
   * @param newAdIdOpt Костыль: если была добавлена рекламная карточка, то она должна отобразится сразу,
   *                   независимо от refresh в индексе. Тут её id.
   */
  def showAdnNode(adnId: String, newAdIdOpt: Option[String]) = IsAdnNodeAdmin(adnId).async { implicit request =>
    import request.adnNode
    // Супервайзинг узла приводит к рендеру ещё одного виджета
    val slavesFut: Future[Seq[MAdnNode]] = if(request.adnNode.adn.isSupervisor) {
      MAdnNode.findBySupId(adnId)
    } else {
      Future successful Nil
    }
    val adsFut = MAd.findForProducerRt(adnId)
    // Бывает, что добавлена новая карточка (но индекс ещё не сделал refresh). Нужно её найти и отобразить:
    val extAdOptFut = newAdIdOpt match {
      case Some(newAdId) => MAd.getById(newAdId)
        .map { _.filter { mad =>
        mad.producerId == adnId  ||  mad.receivers.valuesIterator.exists(_.receiverId == adnId)
      } }
      case None => Future successful None
    }
    // Дождаться всех фьючерсов и отрендерить результат.
    for {
      mads      <- adsFut
      extAdOpt  <- extAdOptFut
      slaves    <- slavesFut
    } yield {
      // Если есть карточка в extAdOpt, то надо добавить её в начало списка, который отсортирован по дате создания.
      val mads2 = if (extAdOpt.isDefined  &&  mads.headOption.flatMap(_.id) != newAdIdOpt) {
        extAdOpt.get :: mads
      } else {
        mads
      }
      Ok(adnNodeShowTpl(adnNode, mads2, slaves))
    }
  }

  
  
  /**
   * Рендер страницы со списком подчинённых узлов.
   * @param adnId id ТЦ
   * @param sortByRaw Сортировка магазинов по указанному полю. Если не задано, то порядок не определён.
   * @param isReversed Если true, то будет сортировка в обратном порядке. Иначе в прямом.
   */
  def showSlaves(adnId: String, sortByRaw: Option[String], isReversed: Boolean) = IsAdnNodeAdmin(adnId).async { implicit request =>
    val sortBy = sortByRaw.flatMap(NodesSort.handleShopsSortBy)
    MAdnNode.findBySupId(adnId, sortBy, isReversed) map { slaves =>
      Ok(slaveNodesTpl(request.adnNode, slaves))
    }
  }
  
    /** Поисковая форма. Сейчас в шаблонах она не используется, только в контроллере. */
  val searchFormM = Form(
    "q" -> nonEmptyText(maxLength = 64)
  )

  /**
   * Поиск по под-узлам указанного супервизора.
   * @param adnId id ТЦ.
   * @return 200 Отрендеренный список узлов для отображения поверх существующей страницы.
   *         406 С сообщением об ошибке.
   */
  def searchSlaves(adnId: String) = IsAdnNodeAdmin(adnId).async { implicit request =>
    searchFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"searchSlaves($adnId): Failed to bind search form: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad search request")
      },
      {q =>
        MAdnNode.searchAll(q, supId = Some(adnId)) map { slaves =>
          Ok(_slaveNodesListTpl(slaves))
        }
      }
    )
  }


  // Допустимые значения сортировки при выдаче магазинов.
  object NodesSort extends Enumeration {
    val SORT_BY_A_Z   = Value("a-z")
    val SORT_BY_CAT   = Value("cat")
    val SORT_BY_FLOOR = Value("floor")

    def handleShopsSortBy(sortRaw: String): Option[String] = {
      if (SORT_BY_A_Z.toString equalsIgnoreCase sortRaw) {
        Some(EsModel.NAME_ESFN)
      } else if (SORT_BY_CAT.toString equalsIgnoreCase sortRaw) {
        debug(s"handleShopsSortBy($sortRaw): Not yet implemented.")
        None
      } else if (SORT_BY_FLOOR.toString equalsIgnoreCase sortRaw) {
        Some(META_FLOOR_ESFN)
      } else {
        None
      }
    }
  }



  /** Маппинг формы включения/выключения магазина. */
  private val nodeOnOffFormM = Form(tuple(
    "isEnabled" -> boolean,
    "reason"    -> optional(hideEntityReasonM)
  ))


  /**
   * Рендер блока с формой отключения подчинённого узла.
   * @param adnId id отключаемого узла.
   * @return 200 с формой указания причины отключения узла.
   *         404 если узел не найден.
   */
  def nodeOnOffForm(adnId: String) = CanSuperviseNode(adnId).apply { implicit request =>
    import request.slaveNode
    val formBinded = nodeOnOffFormM.fill((false, slaveNode.adn.disableReason))
    Ok(_nodeOnOffFormTpl(slaveNode, formBinded))
  }

  /**
   * Супервизор подсети включает/выключает состояние узла.
   * @param shopId id узла.
   * @return 200 Ok если всё ок.
   */
  def nodeOnOffSubmit(shopId: String) = CanSuperviseNode(shopId).async { implicit request =>
    nodeOnOffFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"nodeOnOffSubmit($shopId): Bind form failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Bad request body.")
      },
      {case (isEnabled, reason) =>
        request.slaveNode.setIsEnabled(isEnabled, reason) map { _ =>
          val reply = JsObject(Seq(
            "isEnabled" -> JsBoolean(isEnabled),
            "shopId" -> JsString(shopId)
          ))
          Ok(reply)
        }
      }
    )
  }
  
  
  
  /** Форма, которая используется при обработке сабмита о переключении доступности магазину функции отображения рекламы
    * на верхнем уровне ТЦ. */
  private val nodeTopLevelFormM = Form(
    "isEnabled" -> boolean
  )

  /** Владелец ТЦ дергает за переключатель доступности top-level выдачи для магазина. */
  def setSlaveTopLevelAvailable(adnId: String) = CanSuperviseNode(adnId).async { implicit request =>
    nodeTopLevelFormM.bindFromRequest().fold(
      {formWithErrors =>
        debug(s"shopSetTopLevel($adnId): Form bind failed: ${formatFormErrors(formWithErrors)}")
        NotAcceptable("Cannot parse req body.")
      },
      {isTopEnabled =>
        import request.slaveNode
        if (isTopEnabled)
          slaveNode.adn.showLevelsInfo.out += AdShowLevels.LVL_START_PAGE -> 1
        else
          slaveNode.adn.showLevelsInfo.out -= AdShowLevels.LVL_START_PAGE
        slaveNode.save map { _ =>
          Ok("updated ok")
        }
      }
    )
  }



  /**
   * Отобразить страницу по подчинённому узлу.
   * @param adnId id под-узла.
   */
  def showSlave(adnId: String) = CanViewSlave(adnId).async { implicit request =>
    import request.{slaveNode, supNode}
    val req = AdSearch(
      receiverIds = List(request.supNode.id.get),
      producerIds = List(adnId)
    )
    MAd.searchAdsRt(req) map { mads =>
      Ok(showSlaveNodeTpl(msup = supNode, mslave = slaveNode, mads))
    }
  }

}
