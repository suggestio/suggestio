package util.acl

import javax.inject.Inject
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.es.model.EsModel
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImplLazy
import japgolly.univeq._
import models.mup.{MUploadReq, MUploadTargetQs}
import models.req.MSioUsers
import play.api.http.{HttpErrorHandler, Status}
import play.api.inject.Injector
import play.api.mvc._
import util.cdn.CdnUtil
import util.up.UploadUtil

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:34
  * Description: ACL-проверка на предмет возможности текущему юзеру производить заливку файла в suggest.io.
  */
class CanUploadFile @Inject()(
                               injector                   : Injector,
                               uploadUtil                 : UploadUtil,
                               implicit private val ec    : ExecutionContext,
                             )
  extends MacroLogsImplLazy
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val reqUtil = injector.instanceOf[ReqUtil]
  private lazy val mSioUsers = injector.instanceOf[MSioUsers]
  private lazy val defaultActionBuilder = injector.instanceOf[DefaultActionBuilder]
  private lazy val cdnUtil = injector.instanceOf[CdnUtil]
  private lazy val mCtxIds = injector.instanceOf[MCtxIds]
  private lazy val httpErrorHandler = injector.instanceOf[HttpErrorHandler]


  /** Логика кода проверки прав, заворачивающая за собой фактический экшен, живёт здесь.
    * Это позволяет использовать код и в ActionBuilder, и в Action'ах.
    *
    * Проверка исполняется ВНЕ ПОЛЬЗОВАТЕЛЬСКОЙ СЕССИИ, на нодах *.nodes.suggest.io.
    *
    * @param upTg Описание данных аплоада.
    * @param request0 HTTP-реквест.
    * @param f Фунция фактического экшена.
    * @tparam A Тип BodyParser'а.
    * @return Фьючерс результата.
    */
  private def _apply[A](upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId], request0: Request[A])
                       (f: MUploadReq[A] => Future[Result]): Future[Result] = {

    lazy val logPrefix = s"[${System.currentTimeMillis}]:"

    val uploadNow = uploadUtil.rightNow()

    // Сразу проверяем ttl, до user.isSuper, чтобы суперюзеры могли тоже увидеть возможные проблемы.
    if ( !uploadUtil.isTtlValid(upTg.validTillS, uploadNow) ) {
      // TTL upload-ссылки истёк. Огорчить юзера.
      val msg = "URL TTL expired"
      LOGGER.warn(s"$logPrefix $msg: ${upTg.validTillS}; now was == $uploadNow")
      httpErrorHandler.onClientError(request0, Status.NOT_ACCEPTABLE, msg)

    } else {
      // 2017.oct.19 Для кукисов затянуты гайки, и они теперь точно не передаются на ноды. Берём данные сессии прямо из подписанного URL запроса.
      val user = mSioUsers( upTg.personId )
      if (ctxIdOpt.exists(ctxId => !mCtxIds.validate(ctxId, user.personIdOpt))) {
        val ctxId = ctxIdOpt.get
        // Юзер прислал неправильный ctxId. Такое возможно, если юзер перелогинился в одной вкладке, но не в другой. Либо попытка подмены.
        val msg = "CtxId is not valid."
        LOGGER.warn(s"$logPrefix $msg for user#${user.personIdOpt.orNull}, userMatchesCtxId?${user.personIdOpt ==* ctxId.personId}, raw ctxId = $ctxId")
        httpErrorHandler.onClientError(request0, Status.FORBIDDEN, msg)

      } else {
        // Если задан id узла для обновления, то поискать узел:
        val existNodeOptFut = upTg.info.existNodeId.fold {
          if (upTg.info.nodeType.isEmpty) {
            val msg = s"$logPrefix nodeType is empty, but existNodeId is empty. Node creation impossible w/o type."
            Future.failed( new IllegalStateException(msg) )
          } else {
            Future.successful( Option.empty[MNode] )
          }
        } { nodeId =>
          import esModel.api._

          for {
            mnodeOpt <- mNodes.getByIdCache( nodeId )
            if {
              val r = mnodeOpt.nonEmpty
              if (!r)
                LOGGER.error(s"$logPrefix Requested node#$nodeId not found.")
              r
            }
            mnode = mnodeOpt.get
            if {
              val wantedType = (upTg.info.nodeType getOrElse MNodeTypes.Media)
              val r = mnode.common.ntype eqOrHasParent wantedType

              if (r)
                LOGGER.trace(s"$logPrefix Target node#$nodeId found with expected type#${upTg.info.nodeType.fold("*")(_.toString)}")
              else
                LOGGER.error(s"$logPrefix Node#$nodeId has unexpected type#${mnode.common.ntype}. Should be #$wantedType")

              r
            }
          } yield {
            mnodeOpt
          }
        }

        (for {
          storageEith <- cdnUtil.checkStorageForThisNode( upTg.storage.storage )
          if storageEith.isRight
          existNodeOpt <- existNodeOptFut
          resp <- {
            LOGGER.trace(s"$logPrefix Allowed to process file upload, storage => $storageEith")
            val mreq = MUploadReq(
              swfsOpt       = storageEith.toOption.flatten,
              existNodeOpt  = existNodeOpt,
              request       = request0,
              user          = user,
            )
            f(mreq)
          }
        } yield {
          resp
        })
          .recoverWith { case ex: Throwable =>
            // Рядом с текущим узлом нет искомой swfs volume. Это значит, что юзер подменил хостнейм в сгенеренной ссылке,
            // и пытается залить файл мимо целевого сервера (либо какая-то ошибка в конфигурации).
            LOGGER.warn(s"$logPrefix Failed to validate SWFS upload args", ex)
            httpErrorHandler.onClientError(request0, Status.EXPECTATION_FAILED, s"Storage ${upTg.storage}:${upTg.storage.host.nameInt}:${upTg.storage.storage} looks unavailable for upload from ${uploadUtil.MY_NODE_PUBLIC_URL}.")
          }
      }
    }

  }


  /** Сборка ActionBuilder'а, проверяющего возможность для аплоада файла. */
  def apply(upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId]): ActionBuilder[MUploadReq, AnyContent] = {
    new reqUtil.SioActionBuilderImpl[MUploadReq] {
      override def invokeBlock[A](request: Request[A], block: (MUploadReq[A]) => Future[Result]): Future[Result] = {
        _apply(upTg, ctxIdOpt, request)(block)
      }
    }
  }


  /** Сборка заворачивающего экшена, который проверяет возможность для аплоада файла. */
  def A[A](upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId])(action: Action[A]): Action[A] = {
    defaultActionBuilder.async(action.parser) { request =>
      _apply(upTg, ctxIdOpt, request)(action.apply)
    }
  }

}
