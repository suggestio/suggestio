package controllers.sc

import _root_.util.jsa.{JsAppendById, JsAction, SmRcvResp, Js}
import models.jsm.ProducerAdsResp
import models.msc.{IhBtnArgs, HBtnArgs, FocusedAdsTplArgs, ScJsState}
import play.twirl.api.Html
import util.showcase._
import util.SiowebEsUtil.client
import util.PlayMacroLogsI
import util.acl._
import views.html.sc._
import play.api.libs.json._
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.collection.immutable
import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.11.14 19:38
 * Description: Поддержка открытых рекламных карточек.
 */
trait ScFocusedAds extends ScController with PlayMacroLogsI {


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
      override def _scStateOpt = None

      /** Рендер заэкранного блока. Тут нужен JsString. */
      override def renderOuterBlock(madsCountInt: Int, brArgs: blk.RenderArgs, index: Int, producer: MAdnNode): Future[OBT] = {
        renderBlockHtml(madsCountInt = madsCountInt, brArgs = brArgs, index = index, producer = producer)
          .map { html => JsString(html) }
      }
    }
    // Запускаем сборку ответа:
    val focAdHtmlOptFut = logic.focAdHtmlOptFut
      .map(_.map(JsString(_)))
    val smRcvRespFut = for {
      outerBlocksRendered <- logic.blocksHtmlsFut
      focAdHtmlOpt        <- focAdHtmlOptFut
    } yield {
      SmRcvResp(ProducerAdsResp(focAdHtmlOpt, outerBlocksRendered))
    }
    // Запуск сборки css-инжекции в <head> клиента:
    val cssInjectFut = logic.jsAppendAdsCss
    // Итоговый результат выполнения запроса собирается тут.
    val resultFut = for {
      smRcvResp <- smRcvRespFut
      cssInject <- cssInjectFut
    } yield {
      cacheControlShort {
        Ok( Js(10000, cssInject, smRcvResp) )
      }
    }
    // В фоне, когда поступят карточки, нужно будет сохранить по ним статистику:
    logic.mads2Fut onSuccess { case mads =>
      ScFocusedAdsStatUtil(adSearch, mads.flatMap(_.id), withHeadAd = withHeadAd).saveStats
    }
    resultFut
  }


  /** Логика обработки запросов сбора данных по рекламным карточкам и компиляции оных в результаты выполнения запросов. */
  trait FocusedAdsLogic extends AdCssRenderArgs {
    
    /** Параллельный рендер блоков, находящихся за пределом экрана, должен будет возращать результат этого типа для каждого блока. */
    type OBT

    // TODO Не искать вообще карточки, если firstIds.len >= adSearch.size
    // TODO Выставлять offset для поиска с учётом firstIds?

    def _adSearch: AdSearch
    def _scStateOpt: Option[ScJsState]
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
          override def _dsArgsUnderlying: AdSearch = _adSearch
          override def forceFirstIds = Nil
          override def withoutIds = _adSearch.forceFirstIds
        }
      }
      MAd.dynSearch(adSearch2)
    }

    /** В countAds() можно отправлять и обычный adSearch: forceFirstIds там игнорируется. */
    def madsCountFut: Future[Long] = {
      MAd.dynCount(_adSearch)
    }
    lazy val madsCountIntFut = madsCountFut.map(_.toInt)

    /**
     * 2014.jan.28: Если не найдены какие-то элементы, то сообщить об этом в логи.
     * Это нужно для более быстрого выявления проблем с валидными ссылками на несуществующие карточки.
     * @param mads найденные рекламные карточки.
     * @param ids id запрошенных рекламных карточек.
     */
    protected def logMissingFirstIds(mads: Seq[MAd], ids: Seq[String]): Unit = {
      if (mads.size != ids.size) {
        // Выявить, какие id не были найдены.
        val idsFound = mads.iterator.flatMap(_.id).toSet
        val idsWant = ids.toSet
        val idsNotFound = idsWant -- idsFound
        val sb = new StringBuilder(128, "logInvalidFirstIds(): Client requested inexisting ad ids: ")
        idsNotFound.foreach { id =>
          sb.append(id).append(',').append(' ')
        }
        sb.setLength(sb.length - 2)
        LOGGER.debug(sb.toString())
      }
    }

    /** Если выставлены forceFirstIds, то нужно подолнительно запросить получение указанных
      * id карточек и выставить их в начало списка mads1. */
    lazy val mads2Fut: Future[Seq[MAd]] = {
      // Гарантия фонового вычисления mads1Fut:
      val _mads1Fut = mads1Fut
      if (_adSearch.forceFirstIds.nonEmpty) {
        // Если заданы firstIds и offset == 0, то нужно получить из модели указанные рекламные карточки.
        val firstAdsFut = if (_adSearch.offset <= 0) {
          val ids = _adSearch.forceFirstIds
          val fut = MAd.multiGet(ids)
          fut onSuccess { case mads =>
            logMissingFirstIds(mads, ids)
          }
          fut
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

    lazy val mads2andBrArgsFut: Future[Seq[blk.RenderArgs]] = {
      mads2Fut flatMap { mads =>
        val _ctx = ctx
        val _addCssClasses = addCssClasses
        Future.traverse(mads.zipWithIndex) { case (mad, i) =>
          ShowcaseUtil.focusedBrArgsFor(mad)(_ctx)
            .map { brArgs =>
              val brArgs1 = brArgs.copy(
                inlineStyles    = false,
                cssClasses      = _addCssClasses,
                // 2015.mar.06: FIXME Это значение сейчас перезаписывается таким же через showcase.js.
                blockStyle      = brArgs.wideBg.map(_ => "position: absolute; top: 50px; left: 50%;")
              )
              brArgs1 -> i
            }
        } map { resUnsorted =>
          // Восстановить исходный порядок:
          resUnsorted
            .sortBy(_._2)
            .map(_._1)
        }
      }
    }


    def mads4blkRenderFut: Future[Seq[blk.RenderArgs]] = {
      mads2andBrArgsFut.map { mads =>
        if (_withHeadAd) mads.tail else mads // Caused by: java.lang.UnsupportedOperationException: tail of empty list
      }
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
    
    def renderBlocks(madsCountInt: Int, mads4blkRender: Seq[blk.RenderArgs], producersMap: Map[String, MAdnNode]): Future[Seq[OBT]] = {
      parTraverseOrdered(mads4blkRender, startIndex = _adSearch.offset) {
        (madAndArgs, index) =>
          renderOuterBlock(
            madsCountInt  = madsCountInt,
            brArgs    = madAndArgs,
            index         = index,
            // TODO Нужно parTraverseOrdered() реализовать как flatMap (а не map), и тут можно добавить обработку отсутсвующего продьюсера.
            producer      = producersMap(madAndArgs.mad.producerId)
          )
      }
    }
    
    def renderBlockHtml(madsCountInt: Int, brArgs: blk.RenderArgs, index: Int, producer: MAdnNode): Future[Html] = {
      Future {
        _focusedAdTpl(brArgs, index + 1, producer, adsCount = madsCountInt)(ctx)
      }
    }
    
    /** Рендер заэкранного блока. В случае Html можно просто вызвать renderBlockHtml(). */
    def renderOuterBlock(madsCountInt: Int, brArgs: blk.RenderArgs, index: Int, producer: MAdnNode): Future[OBT]

    /** Что же будет рендерится в качестве текущей просматриваемой карточки? */
    lazy val focAdOptFut: Future[Option[blk.RenderArgs]] = {
      mads2andBrArgsFut.map(_.headOption)
    }

    /** Фьючерс продьюсера, относящегося к текущей карточке. */
    def focAdProducerOptFut: Future[Option[MAdnNode]] = {
      val _prodsMapFut = mads2ProducersFut
      focAdOptFut flatMap {
        case Some(focMad) =>
          _prodsMapFut map { prodsMap =>
            prodsMap.get(focMad.mad.producerId)
          }
        case None =>
          Future successful None
      }
    }

    /** Узнать продьюсера отображаемой рекламной карточки. */
    def focMadProducerOptFut: Future[Option[MAdnNode]] = {
      focAdOptFut flatMap { madAndArgsOpt =>
        madProducerOptFut(madAndArgsOpt.map(_.mad))
      }
    }

    def madProducerOptFut(madOpt: Option[MAd]): Future[Option[MAdnNode]] = {
      val prodIdOpt = madOpt.map(_.producerId)
      MAdnNodeCache.maybeGetByIdCached(prodIdOpt)
    }

    /** Сборка контейнера аргументов для вызова шаблона _focusedAdsTpl(). */
    def focAdsHtmlArgsFut: Future[FocusedAdsTplArgs] = {
      val _producerFut = focAdProducerOptFut.map(_.get)
      val _brArgsFut = focAdOptFut.map(_.get)
      val _madsCountIntFut = madsCountIntFut
      for {
        _producer     <- _producerFut
        _brArgs       <- _brArgsFut
        madsCountInt  <- _madsCountIntFut
      } yield {
        val _bgColor = _producer.meta.color   getOrElse ShowcaseUtil.SITE_BGCOLOR_DFLT
        val _fgColor = _producer.meta.fgColor getOrElse ShowcaseUtil.SITE_FGCOLOR_DFLT
        val _hBtnArgs = HBtnArgs(fgColor = _fgColor)
        new FocusedAdsTplArgs {
          override def producer   = _producer
          override def bgColor    = _bgColor
          override def fgColor    = _fgColor
          override def hBtnArgs   = _hBtnArgs
          override def brArgs     = _brArgs
          override def adsCount   = madsCountInt
          override def startIndex = _adSearch.offset
          override def jsStateOpt = _scStateOpt
        }
      }
    }

    override def adsCssExternalFut: Future[Seq[AdCssArgs]] = {
      mads2andBrArgsFut.map { mbas =>
        mbas.map { mba =>
          AdCssArgs(mba.mad.id.get, mba.szMult)
        }
      }
    }

    override def adsCssFieldRenderArgsFut: Future[immutable.Seq[blk.FieldCssRenderArgsT]] = {
      mads2andBrArgsFut.map { mbas =>
        mbas.iterator
          .flatMap { mba =>  mad2craIter(mba, mba.cssClasses) }
          .toStream
      }
    }

    /** Дописывать эти css-классы в стили и в рендер. */
    def addCssClasses = Seq("focused")

    /** Параметры для рендера обрамляющего css блоков (css не полей, а блоков в целом). */
    override def adsCssRenderArgsFut: Future[immutable.Seq[blk.IRenderArgs]] = {
      mads2andBrArgsFut.map { mbas =>
        mbas.toStream
      }
    }

    /** Отрендеренное отображение раскрытой карточки вместе с обрамлениями и остальным.
      * Т.е. пригодно для вставки в соотв. div indexTpl. Функция игнорирует значение [[_withHeadAd]].
      * @return Если нет карточек, то будет NoSuchElementException. Иначе фьючерс с HTML-рендером. */
    def focAdHtmlFut: Future[Html] = {
      focAdsHtmlArgsFut map { args =>
        _focusedAdsTpl(args)(ctx)
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

    override def jsAppendCssAction(html: JsString): JsAction = {
      JsAppendById("smResourcesFocused", html)
    }
  }

}
