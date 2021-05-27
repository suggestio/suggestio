package controllers

import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.{EsModel, MEsUuId}
import io.suggest.img.{MImgFmtJvm, MImgFormats}
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.n2.edge.MEdge
import io.suggest.n2.node.{MNode, MNodes}
import io.suggest.sc.ads.MAdsSearchReq
import io.suggest.sc.sc3.{MScCommonQs, MScQs}
import io.suggest.sec.util.Csrf
import io.suggest.util.logs.MacroLogsImplLazy

import javax.inject.Inject
import models.blk.{OneAdQsArgs, OneAdWideQsArgs}
import models.mctx.Context
import models.mproj.ICommonDi
import models.msc.{OneAdRenderVariant, OneAdRenderVariants}
import models.msys.{MImgEdge, MShowAdRcvrsTplArgs, MShowAdTplArgs, MShowNodeAdsTplArgs, MShowOneAdFormTplArgs, MSysNodeInstallFormData}
import models.req.{IAdReq, INodeReq}
import play.api.data.{Form, Mapping}
import play.api.inject.Injector
import play.api.mvc.{Call, Result}
import util.FormUtil
import util.acl.{IsSu, IsSuMad, IsSuNode, SioControllerApi}
import util.adn.NodesUtil
import util.adv.direct.AdvRcvrsUtil
import util.lk.LkAdUtil
import util.n2u.N2NodesUtil
import util.showcase.ScAdSearchUtil
import util.sys.SysMarketUtil
import views.html.sys1.market.ad.one._
import views.html.sys1.market.ad.{showAdRcvrsTpl, showAdTpl, showAdnNodeAdsTpl}
import views.html.sys1.market.ad.install.installDfltMadsTpl

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.04.15 15:50
 * Description: sys-раздел для отладки рендера карточек в картинки или html.
 */
final class SysAd @Inject()(
                             injector             : Injector,
                             sioControllerApi     : SioControllerApi,
                           )
  extends MacroLogsImplLazy
{

  private lazy val isSuMad = injector.instanceOf[IsSuMad]
  private lazy val isSu = injector.instanceOf[IsSu]
  private lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private val csrf = injector.instanceOf[Csrf]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private lazy val advRcvrsUtil = injector.instanceOf[AdvRcvrsUtil]
  private lazy val scAdSearchUtil = injector.instanceOf[ScAdSearchUtil]
  private lazy val lkAdUtil = injector.instanceOf[LkAdUtil]
  private lazy val mCommonDi = injector.instanceOf[ICommonDi]
  private lazy val mItems = injector.instanceOf[MItems]

  private lazy val sysMarketUtil = injector.instanceOf[SysMarketUtil]
  private lazy val isSuNode = injector.instanceOf[IsSuNode]
  private lazy val nodesUtil = injector.instanceOf[NodesUtil]

  import sioControllerApi._


  /**
   * Рендер страницы с формой забивания значений [[models.blk.OneAdQsArgs]].
   *
   * @param madId id текущей рекламной карточки.
   * @param rvar Интересующий render variant.
   * @return 200: Страница с формой.
   *         Редиректы в остальных случаях.
   *         404 если указанная карточка не найдена.
   */
  def showOneAdForm(madId: String, rvar: OneAdRenderVariant) = csrf.AddToken {
    isSuMad(madId).async { implicit request =>
      // Забиндить форму дефолтовыми данными для отправки в шаблон.
      val formArgs = OneAdQsArgs(
        adId    = madId,
        szMult  = 1.0F,
        vsnOpt  = request.mad.versioning.version,
        imgFmt  = MImgFormats.JPEG,
        wideOpt = for (bm <- request.mad.ad.blockMeta) yield {
          OneAdWideQsArgs(
            width = bm.width * 2
          )
        }
      )
      val qf = oneAdQsArgsFormM(madId)
        .fill( formArgs )
      // Запустить рендер.
      _showOneAdFormRender(qf, rvar, Ok)
    }
  }

  private def _showOneAdFormRender(qf: Form[OneAdQsArgs], rvar: OneAdRenderVariant, rs: Status)
                                  (implicit request: IAdReq[_]): Future[Result] = {
    import esModel.api._

    val producerIdOpt = n2NodesUtil.madProducerId(request.mad)
    val nodeOptFut = mNodes.maybeGetByIdCached( producerIdOpt )
    for {
      nodeOpt <- nodeOptFut
    } yield {
      val rargs = MShowOneAdFormTplArgs(request.mad, rvar, qf, nodeOpt)
      val html = argsFormTpl(rargs)
      rs( html )
    }
  }


  /**
   * Сабмит формы запроса рендера карточки.
   *
   * @param madId id карточки.
   * @param rvar Интересующий render variant.
   * @return Редирект на результат рендера карточки согласно переданным параметрам.
   */
  def oneAdFormSubmit(madId: String, rvar: OneAdRenderVariant) = csrf.Check {
    isSuMad(madId).async { implicit request =>
      oneAdQsArgsFormM(madId).bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(s"oneAdFormSubmit($madId, ${rvar.nameI18n}): Failed to bind form:\n ${formatFormErrors(formWithErrors)}")
          _showOneAdFormRender(formWithErrors, rvar, NotAcceptable)
        },
        {oneAdQsArgs =>
          val call = _oneAdRenderCall(rvar, oneAdQsArgs)
          Redirect( call )
        }
      )
    }
  }


  /** Сборка ссылки требуемый на рендер карточки.
    *
    * @param rvar Вариант рендера.
    * @param qsArgs Парамерты рендера.
    * @return Инстанс Call.
    */
  private def _oneAdRenderCall(rvar: OneAdRenderVariant, qsArgs: OneAdQsArgs): Call = {
    import controllers.sc.routes.ScOnlyOneAd
    rvar match {
      case OneAdRenderVariants.ToHtml =>
        ScOnlyOneAd.onlyOneAd( qsArgs )
      case OneAdRenderVariants.ToImage =>
        ScOnlyOneAd.onlyOneAdAsImage( qsArgs )
    }
  }


  /** Отобразить технический список рекламных карточек узла. */
  def showAdnNodeAds(a: MScQs) = csrf.AddToken {
    isSu().async { implicit request =>
      import esModel.api._

      // Ищем все рекламные карточки, подходящие под запрос.
      // TODO Нужна устойчивая сортировка.
      val msearch = scAdSearchUtil.qsArgs2nodeSearch(a)
      val madsFut = mNodes.dynSearch( msearch )

      val brArgsFut = madsFut.flatMap { mads =>
        Future.traverse(mads) { mad =>
          lkAdUtil.tiledAdBrArgs(mad)
        }
      }

      def __nodeIdsF(x: Option[MEsUuId]): Seq[String] = {
        x.iterator.map(_.id).toSeq
      }
      val producerIds = __nodeIdsF( a.search.prodId )
      val rcvrIds     = __nodeIdsF( a.search.rcvrId )

      // Узнаём текущий узел на основе запроса. TODO Кривовато это как-то, может стоит через аргумент передавать?
      val adnNodeIdOpt = {
        producerIds
          .headOption
          .orElse {
            rcvrIds.headOption
          }
      }

      val adnNodeOptFut = FutureUtil.optFut2futOpt(adnNodeIdOpt)( mNodes.getByIdCache )

      // Собираем карту размещений рекламных карточек.
      val ad2advMapFut: Future[Map[String, Seq[MItem]]] = {
        for {
          mads <- madsFut
          advs <- {
            import mCommonDi.slick
            import slick.profile.api._

            lazy val adIds = mads.flatMap(_.id)
            val q0 = {
              val statuses = MItemStatuses.advBusyIds.toSeq
              mItems.query
                .filter { i =>
                  (i.nodeId inSet adIds) && (i.statusStr inSet statuses)
                }
            }
            val items: Future[Seq[MItem]] = if (rcvrIds.nonEmpty) {
              // Ищем все размещения имеющихся карточек у запрошенных ресиверов.
              slick.db.run {
                q0.filter(_.rcvrIdOpt inSet rcvrIds).result
              }

            } else if (producerIds.nonEmpty) {
              // Ищем размещения карточек для продьюсера.
              slick.db.run {
                q0.result
              }

            } else {
              Future.successful(Nil)
            }
            items
          }
        } yield {
          advs.groupBy(_.nodeId)
        }
      }

      // Собираем ресиверов рекламных карточек.
      val rcvrsFut: Future[Map[String, Seq[MNode]]] = if (rcvrIds.nonEmpty) {
        // Используем только переданные ресиверы.
        Future
          .traverse(rcvrIds) { mNodes.getByIdCache }
          .flatMap { rcvrOpts =>
            val rcvrs = rcvrOpts.flatten
            madsFut map { mads =>
              mads.flatMap(_.id)
                .map { adId => adId -> rcvrs }
                .toMap
            }
          }

      } else {
        // Собираем всех ресиверов со всех рекламных карточек. Делаем это через биллинг, т.к. в mad только текущие ресиверы.
        for {
          ad2advsMap  <- ad2advMapFut
          allRcvrs    <- {
            val allRcvrIdsSet = ad2advsMap
              .valuesIterator
              .flatten
              .flatMap(_.rcvrIdOpt)
              .toSet
            mNodes.multiGetCache(allRcvrIdsSet)
          }

        } yield {
          // Список ресиверов конвертим в карту ресиверов.
          val rcvrsMap = allRcvrs
            .iterator
            .map { rcvr =>
              rcvr.id.get -> rcvr
            }
            .toMap
          // Заменяем в исходной карте ad2advs списки adv на списки ресиверов.
          ad2advsMap
            .view
            .mapValues { advs =>
              advs.flatMap { adv =>
                adv.rcvrIdOpt
                  .flatMap(rcvrsMap.get)
              }
            }
            .toMap
        }
      }

      // Планируем рендер страницы-результата, когда все данные будут собраны.
      for {
        brArgss       <- brArgsFut
        adnNodeOpt    <- adnNodeOptFut
        rcvrs         <- rcvrsFut
        ad2advMap     <- ad2advMapFut
      } yield {
        val rargs = MShowNodeAdsTplArgs(brArgss, adnNodeOpt, rcvrs, a, ad2advMap, msearch)
        Ok( showAdnNodeAdsTpl(rargs) )
      }
    }
  }


  /** Убрать указанную рекламную карточку из выдачи указанного ресивера. */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String], r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      // Запускаем спиливание ресивера для указанной рекламной карточки.
      val madSavedFut = advRcvrsUtil.depublishAdOn(request.mad, rcvrIdOpt.toSet)

      lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
      // Радуемся в лог.
      rcvrIdOpt.fold {
        LOGGER.warn(logPrefix + "Starting removing ALL rcvrs...")
      } { _ =>
        LOGGER.info(logPrefix + "Starting removing for single rcvr...")
      }

      // Начинаем асинхронно генерить ответ клиенту.
      val rdrToFut: Future[Result] = RdrBackOrFut(r) {
        val call = rcvrIdOpt.fold[Call] {
          n2NodesUtil.madProducerId(request.mad)
            .fold( routes.SysMarket.index() ) { prodId =>
              routes.SysMarket.showAdnNode(prodId)
            }
        } { rcvrId =>
          val adSearch = MScQs(
            search = MAdsSearchReq(
              rcvrId = Some( rcvrId )
            ),
            common = MScCommonQs.empty
          )
          routes.SysAd.showAdnNodeAds(adSearch)
        }
        Future.successful( call )
      }

      // Дождаться завершения всех операций.
      for {
        rdr  <- rdrToFut
        _    <- madSavedFut
      } yield {
        // Вернуть редирект с результатом работы.
        rdr.flashing( FLASH.SUCCESS -> "Карточка убрана из выдачи." )
      }
    }
  }


  /**
    * Выдать sys-страницу относительно указанной карточки.
    *
    * @param adId id рекламной карточки.
    */
  def showAd(adId: String) = csrf.AddToken {
    isSuMad(adId).async { implicit request =>
      import request.mad
      import esModel.api._

      // Определить узла-продьюсера
      val producerIdOpt = n2NodesUtil.madProducerId( mad )
      val producerOptFut = mNodes.maybeGetByIdCached( producerIdOpt )

      // Собрать инфу по картинкам.
      // TODO Тут код наверное уже не актуален. Просто подправлен тут для совместимости.
      val imgs = List.empty[MImgEdge]
      /*
        mad.edges
          .withPredicateIter( MPredicates.JdContent.Image )
          .map { e =>
            MImgEdge(e, MImg3(e))
          }
          .toSeq
      */

      // Считаем кол-во ресиверов.
      val rcvrsCount = n2NodesUtil.receiverIds(mad)
        .toSet
        .size

      // Вернуть результат, когда всё будет готово.
      for {
        producerOpt <- producerOptFut
      } yield {
        val rargs = MShowAdTplArgs(mad, producerOpt, imgs, producerIdOpt, rcvrsCount)
        Ok( showAdTpl(rargs) )
      }
    }
  }


  /** Вывести результат анализа ресиверов рекламной карточки. */
  def analyzeAdRcvrs(adId: String) = csrf.AddToken {
    isSuMad(adId).async { implicit request =>
      import request.mad
      import esModel.api._

      val producerId = n2NodesUtil.madProducerId(mad).get
      val producerOptFut = mNodes.getByIdCache(producerId)

      val newRcvrsMapFut = for {
        producerOpt <- producerOptFut
        acc2 <- advRcvrsUtil.calculateReceiversFor(mad, producerOpt)
      } yield {
        // Нужна только карта ресиверов. Дроп всей остальной инфы...
        acc2.mnode.edges.out
      }

      val rcvrsMap = n2NodesUtil.receiversMap(mad)

      // Достаём из кэша узлы.
      val nodesMapFut: Future[Map[String, MNode]] = {
        def _nodeIds(rcvrs: Seq[MEdge]): Set[String] = {
          if (rcvrs.nonEmpty) {
            rcvrs.iterator
              .map(_.nodeIds)
              .reduceLeft(_ ++ _)
          } else {
            Set.empty
          }
        }
        val adnIds1 = _nodeIds(rcvrsMap)
        for {
          adns1       <- mNodes.multiGetCache(adnIds1)
          newRcvrsMap <- newRcvrsMapFut
          newAdns     <- {
            val newRcvrIds = _nodeIds(newRcvrsMap)
            mNodes.multiGetCache(newRcvrIds -- adnIds1)
          }
        } yield {
          (adns1 :: newAdns :: Nil)
            .iterator
            .flatten
            .zipWithIdIter[String]
            .to( Map )
        }
      }

      // Узнать, совпадает ли рассчетная карта ресиверов с текущей.
      val rcvrsMapOkFut = for (newRcvrsMap <- newRcvrsMapFut) yield {
        advRcvrsUtil.isRcvrsMapEquals(newRcvrsMap, rcvrsMap)
      }

      for {
        newRcvrsMap <- newRcvrsMapFut
        producerOpt <- producerOptFut
        nodesMap    <- nodesMapFut
        rcvrsMapOk  <- rcvrsMapOkFut
      } yield {
        val rargs = MShowAdRcvrsTplArgs( mad, newRcvrsMap, nodesMap, producerOpt, rcvrsMap, rcvrsMapOk )
        Ok( showAdRcvrsTpl(rargs) )
      }
    }
  }


  /** Пересчитать и сохранить ресиверы для указанной рекламной карточки. */
  def resetReceivers(adId: String, r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      for {
        _ <- advRcvrsUtil.resetReceiversFor(request.mad)
      } yield {
        // Когда всё будет сделано, отредиректить юзера назад на страницу ресиверов.
        RdrBackOr(r) { routes.SysAd.analyzeAdRcvrs(adId) }
          .flashing(FLASH.SUCCESS -> s"Произведён сброс ресиверов узла-карточки.")
      }
    }
  }


  /** Очистить полностью таблицу ресиверов. Бывает нужно для временного сокрытия карточки везде.
    * Это действие можно откатить через resetReceivers. */
  def cleanReceivers(adId: String, r: Option[String]) = csrf.Check {
    isSuMad(adId).async { implicit request =>
      for {
        _ <- advRcvrsUtil.cleanReceiverFor(request.mad)
      } yield {
        RdrBackOr(r) { routes.SysAd.analyzeAdRcvrs(adId) }
          .flashing(FLASH.SUCCESS -> "Из узла вычищены все ребра ресиверов. Биллинг не затрагивался.")
      }
    }
  }


  /** Вернуть страницу с формой установки дефолтовых карточек на узлы. */
  def installDfltMads(adnId: String) = csrf.AddToken {
    isSuNode(adnId).async { implicit request =>
      implicit val ctx = implicitly[Context]
      val fd = MSysNodeInstallFormData(
        count = nodesUtil.INIT_ADS_COUNT,
        lang  = ctx.messages.lang
      )
      val form = sysMarketUtil.nodeInstallForm.fill(fd)
      _installRender(form, Ok)(ctx, request)
    }
  }


  /** Общий код экшенов, связанный с рендером html-ответа. */
  private def _installRender(form: Form[MSysNodeInstallFormData], rs: Status)
                            (implicit ctx: Context, request: INodeReq[_]): Future[Result] = {
    import esModel.api._
    import mCommonDi.langs

    for {
      srcNodes <- mNodes.multiGetCache(nodesUtil.ADN_IDS_INIT_ADS_SOURCE)
    } yield {
      val allLangs = langs.availables
      val langCode2msgs = allLangs.iterator
        .map { l =>
          l.code -> mCommonDi.messagesApi.preferred( l :: Nil )
        }
        .toMap
      val html = installDfltMadsTpl(allLangs, langCode2msgs, request.mnode, form, srcNodes)(ctx)
      rs(html)
    }
  }


  /** Сабмит формы установки дефолтовых карточек. */
  def installDfltMadsSubmit(adnId: String) = csrf.Check {
    isSuNode(adnId).async { implicit request =>
      lazy val logPrefix = s"installDfltMadsSubmit($adnId):"
      sysMarketUtil.nodeInstallForm.bindFromRequest().fold(
        {formWithErrors =>
          LOGGER.debug(logPrefix + "Failed to bind form:\n " + formatFormErrors(formWithErrors) )
          _installRender(formWithErrors, NotAcceptable)
        },
        {fd =>
          val msgs = messagesApi.preferred( fd.lang :: Nil )
          nodesUtil.installDfltMads(adnId, count = fd.count)(msgs)
            .map { madIds =>
              val count = madIds.size
              LOGGER.trace(s"$logPrefix Cloned ok $count mads: [${madIds.mkString(", ")}]")
              Redirect(routes.SysMarket.showAdnNode(adnId))
                .flashing(FLASH.SUCCESS -> s"Клонировано $count дефолтовых карточек.")
            }
        }
      )
    }
  }



  import play.api.data.Forms._

  /** Внутренний маппинг контроллера для OneAdQsArgs. */
  private def oneAdQsArgsM(madId: String): Mapping[OneAdQsArgs] = {
    mapping(
      "szMult" -> FormUtil.szMultM,
      "vsn"    -> FormUtil.esVsnOptM,
      "imgFmt" -> MImgFmtJvm.mapping,
      "wide"   -> OneAdWideQsArgs.optMapper
    )
    { OneAdQsArgs(madId, _, _, _, _) }
    { OneAdQsArgs.unapply(_)
      .map { case (_, a, b, c, d) => (a, b, c, d) }
    }
  }

  /** Маппинг для формы OneAdQsArgs. */
  private def oneAdQsArgsFormM(madId: String): Form[OneAdQsArgs] = {
    Form( oneAdQsArgsM(madId) )
  }

}
