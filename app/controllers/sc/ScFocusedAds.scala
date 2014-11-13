package controllers.sc

import play.twirl.api.Html
import util.showcase._
import util.SiowebEsUtil.client
import util.PlayMacroLogsI
import util.acl._
import views.html.market.showcase._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.14 19:38
 * Description: Поддержка открытых рекламных карточек.
 */
trait ScFocusedAds extends ScController with PlayMacroLogsI with ScSiteConstants {


  /** Экшен для рендера горизонтальной выдачи карточек.
    * @param adSearch Поисковый запрос.
    * @param withHeadAd true означает, что нужна начальная страница с html.
    *          false - возвращать только json-массив с отрендеренными блоками, без html-страницы с первой карточкой.
    * @return JSONP с отрендеренными карточками.
    */
  def focusedAds(adSearch: AdSearch, withHeadAd: Boolean) = MaybeAuth.async { implicit request =>
    val logic = new FocusedAdsLogic {
      override type OBT = JsString

      override def _withHeadAd = withHeadAd
      override def _adSearch = adSearch
      override implicit def _request = request

      /** Рендер заэкранного блока. Тут нужен JsString. */
      override def renderOuterBlock(madsCountInt: Int, mad: MAd, index: Int, producer: MAdnNode): Future[OBT] = {
        renderBlockHtml(madsCountInt = madsCountInt, mad = mad, index = index, producer = producer)
          .map { html => JsString(html) }
      }
    }
    // Запускаем сборку ответа:
    val focAdHtmlOptFut = logic.focAdHtmlOptFut
      .map(_.map(JsString(_)))
    val resultFut = for {
      outerBlocksRendered <- logic.blocksHtmlsFut
      focAdHtmlOpt        <- focAdHtmlOptFut
    } yield {
      cacheControlShort {
        jsonOk("producerAds", focAdHtmlOpt, outerBlocksRendered)
      }
    }
    // В фоне, когда поступят карточки, нужно будет сохранить по ним статистику:
    logic.mads2Fut onSuccess { case mads =>
      ScFocusedAdsStatUtil(adSearch, mads.flatMap(_.id), withHeadAd = withHeadAd).saveStats
    }
    resultFut
  }


  /** Логика обработки запросов сбора данных по рекламным карточкам и компиляции оных в результаты выполнения запросов. */
  trait FocusedAdsLogic {
    
    /** Параллельный рендер блоков, находящихся за пределом экрана, должен будет возращать результат этого типа для каждого блока. */
    type OBT

    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?

    def _adSearch: AdSearch
    def _withHeadAd: Boolean
    implicit def _request: AbstractRequestWithPwOpt[_]

    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?
    lazy val mads1Fut: Future[Seq[MAd]] = {
      // Костыль, т.к. сортировка forceFirstIds на стороне ES-сервера всё ещё не пашет:
      val adSearch2 = if (_adSearch.forceFirstIds.isEmpty) {
        _adSearch
      } else {
        new AdSearchWrapper {
          override def _adsSearchArgsUnderlying: AdSearch = _adSearch
          override def forceFirstIds = Nil
          override def withoutIds = forceFirstIds
        }
      }
      MAd.dynSearch(adSearch2)
    }

    /** В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется. */
    def madsCountFut: Future[Long] = {
      MAd.dynCount(_adSearch)
    }
    lazy val madsCountIntFut = madsCountFut.map(_.toInt)

    // Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных id карточек и выставить их в начало списка mads1.
    lazy val mads2Fut: Future[Seq[MAd]] = {
      // Гарантия фонового вычисления mads1Fut:
      val _mads1Fut = mads1Fut
      if (_adSearch.forceFirstIds.nonEmpty) {
        // Если заданы firstIds и offset == 0, то нужно получить из модели указанные рекламные карточки.
        val firstAdsFut = if (_adSearch.offset <= 0) {
          MAd.multiGet(_adSearch.forceFirstIds)
            .map { _.filter {
              mad => _adSearch.producerIds contains mad.producerId
            } }
        } else {
          Future successful Nil
        }
        // Замёржить полученные first-карточки в основной список карточек.
        for {
          mads      <- _mads1Fut
          firstAds  <- firstAdsFut
        } yield {
          // Нано-оптимизация.
          if (firstAds.nonEmpty)
            firstAds ++ mads
          else
            mads
        }
      } else {
        // Дополнительно выставлять первые карточки не требуется. Просто возвращаем фьючерс исходного списка карточек.
        _mads1Fut
      }
    }


    lazy val mads2ProducersFut: Future[Map[String, MAdnNode]] = {
      mads2Fut.flatMap { mads2 =>
        val producerIds = mads2.iterator.map(_.producerId)
        MAdnNodeCache.multiGet(producerIds)
          .map { ns =>
            ns.iterator
              .map { node => node.id.get -> node }
              .toMap
          }
      }
    }


    def mads4blkRenderFut = mads2Fut.map { mads =>
      if (_withHeadAd) mads.tail else mads // Caused by: java.lang.UnsupportedOperationException: tail of empty list
    }

    lazy val ctx = implicitly[Context]

    def blocksHtmlsFut: Future[Seq[OBT]] = {
      // Форсируем распараллеливание асинхронных операций.
      val _mads4blkRenderFut = mads4blkRenderFut
      val _producersMapFut = mads2ProducersFut
      for {
        madsCountInt   <- madsCountIntFut
        mads4blkRender <- _mads4blkRenderFut
        producersMap   <- _producersMapFut
        rendered       <- renderBlocks(madsCountInt, mads4blkRender, producersMap)
      } yield {
        rendered
      }
    }
    
    def renderBlocks(madsCountInt: Int, mads4blkRender: Seq[MAd], producersMap: Map[String, MAdnNode]): Future[Seq[OBT]] = {
      parTraverseOrdered(mads4blkRender, startIndex = _adSearch.offset) {
        (mad, index) =>
          renderOuterBlock(
            madsCountInt  = madsCountInt,
            mad           = mad,
            index         = index,
            // TODO Нужно parTraverseOrdered() реализовать как flatMap (а не map), и тут можно добавить обработку отсутсвующего продьюсера.
            producer      = producersMap(mad.producerId)
          )
      }
    }
    
    def renderBlockHtml(madsCountInt: Int, mad: MAd, index: Int, producer: MAdnNode): Future[Html] = {
      val _ctx = ctx
      ShowcaseUtil.focusedBrArgsFor(mad)(_ctx) map { brArgs =>
        _focusedAdTpl(mad, index + 1, producer, adsCount = madsCountInt, brArgs = brArgs)(_ctx)
      }
    }
    
    /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
    def renderOuterBlock(madsCountInt: Int, mad: MAd, index: Int, producer: MAdnNode): Future[OBT]

    /** Что же будет рендерится в качестве текущей просматриваемой карточки? */
    lazy val focAdOptFut: Future[Option[MAd]] = {
      mads2Fut.map(_.headOption)
    }

    /** Фьючерс продьюсера, относящегося к текущей карточке. */
    def focAdProducerOptFut: Future[Option[MAdnNode]] = {
      val _prodsMapFut = mads2ProducersFut
      focAdOptFut flatMap {
        case Some(focMad) =>
          _prodsMapFut map { prodsMap =>
            prodsMap.get(focMad.producerId)
          }
        case None =>
          Future successful None
      }
    }

    /** Узнать продьюсера отображаемой рекламной карточки. */
    def focMadProducerOptFut: Future[Option[MAdnNode]] = {
      focAdOptFut flatMap { madProducerOptFut }
    }

    def madProducerOptFut(madOpt: Option[MAd]): Future[Option[MAdnNode]] = {
      val prodIdOpt = madOpt.map(_.producerId)
      MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
    }

    /** Аргументы рендера отфокусированной карточки. */
    def focAdRenderArgsFut: Future[blk.RenderArgs] = {
      focAdOptFut.flatMap { madsHeadOpt =>
        madsHeadOpt.fold
          { Future successful ShowcaseUtil.focusedBrArgsDflt }
          { ShowcaseUtil.focusedBrArgsFor(_)(ctx) }
      }
    }

    /** Отрендеренное отображение раскрытой карточки вместе с обрамлениями и остальным.
      * Т.е. пригодно для вставки в соотв. div indexTpl. Функция игнорирует значение [[_withHeadAd]].
      * @return Если нет карточек, то будет NoSuchElementException. Иначе фьючерс с HTML-рендером. */
    def focAdHtmlFut: Future[Html] = {
      val _producerFut = focAdProducerOptFut.map(_.get)
      val _madsHeadFut = focAdOptFut.map(_.get)
      val _madsCountIntFut = madsCountIntFut
      for {
        brArgs        <- focAdRenderArgsFut
        producer      <- _producerFut
        madsHead      <- _madsHeadFut
        madsCountInt  <- _madsCountIntFut
      } yield {
        val bgColor = producer.meta.color getOrElse SITE_BGCOLOR_DFLT
        _focusedAdsTpl(madsHead, _adSearch, producer, bgColor, brArgs = brArgs, adsCount = madsCountInt,  startIndex = _adSearch.offset)(ctx)
      }
    }

    /** Опциональный аналог focAdHtmlFut. Функция учитывает значение [[_withHeadAd]]. */
    def focAdHtmlOptFut: Future[Option[Html]] = {
      if (_withHeadAd) {
        focAdHtmlFut
          .map(Some.apply)
          .recover {
            case ex: NoSuchElementException =>
              None
            case ex: Throwable =>
              LOGGER.error("Failed to find focused ad", ex)
              None
          }
      } else {
        Future successful None
      }
    }

  }

}
