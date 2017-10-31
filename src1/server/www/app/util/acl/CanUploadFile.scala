package util.acl

import javax.inject.Inject

import io.suggest.common.fut.FutureUtil.HellImplicits._
import io.suggest.ctx.{MCtxId, MCtxIds}
import io.suggest.req.ReqUtil
import io.suggest.util.logs.MacroLogsImpl
import japgolly.univeq._
import models.mproj.ICommonDi
import models.mup.{MUploadReq, MUploadTargetQs}
import models.req.MSioUsers
import play.api.mvc._
import util.cdn.DistUtil
import util.up.UploadUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 04.10.17 11:34
  * Description: ACL-проверка на предмет возможности текущему юзеру производить заливку файла в suggest.io.
  */
class CanUploadFile @Inject()(
                               reqUtil                    : ReqUtil,
                               mSioUsers                  : MSioUsers,
                               uploadUtil                 : UploadUtil,
                               dab                        : DefaultActionBuilder,
                               distUtil                   : DistUtil,
                               mCtxIds                    : MCtxIds,
                               mCommonDi                  : ICommonDi
                             )
  extends MacroLogsImpl
{

  import mCommonDi.ec

  /** Логика кода проверки прав, заворачивающая за собой фактический экшен, живёт здесь.
    * Это позволяет использовать код и в ActionBuilder, и в Action'ах.
    *
    * @param upTg Описание данных аплоада.
    * @param request0 HTTP-реквест.
    * @param f Фунция фактического экшена.
    * @tparam A Тип BodyParser'а.
    * @return Фьючерс результата.
    */
  private def _apply[A](upTg: MUploadTargetQs, ctxIdOpt: Option[MCtxId], request0: Request[A])(f: MUploadReq[A] => Future[Result]): Future[Result] = {

    lazy val logPrefix = s"[${System.currentTimeMillis}]:"

    val uploadNow = uploadUtil.rightNow()

    // Сразу проверяем ttl, до user.isSuper, чтобы суперюзеры могли тоже увидеть возможные проблемы.
    if ( !uploadUtil.isTtlValid(upTg.validTillS, uploadNow) ) {
      // TTL upload-ссылки истёк. Огорчить юзера.
      val msg = "URL TTL expired"
      LOGGER.warn(s"$logPrefix $msg: ${upTg.validTillS}; now was == $uploadNow")
      Results.NotAcceptable(msg)

    } else {
      // 2017.oct.19 Для кукисов затянуты гайки, и они теперь точно не передаются на ноды. Берём данные сессии прямо из подписанного URL запроса.
      val user = mSioUsers( upTg.personId )
      if (ctxIdOpt.exists(ctxId => !mCtxIds.validate(ctxId, user.personIdOpt))) {
        val ctxId = ctxIdOpt.get
        // Юзер прислал неправильный ctxId. Такое возможно, если юзер перелогинился в одной вкладке, но не в другой. Либо попытка подмены.
        val msg = "CtxId is not valid."
        LOGGER.warn(s"$logPrefix $msg for user#${user.personIdOpt.orNull}, userMatchesCtxId?${user.personIdOpt ==* ctxId.personId}, raw ctxId = $ctxId")
        Results.Forbidden(msg)

      } else {
        distUtil
          .checkForUpload( upTg.storage )
          .flatMap { swfsOpt =>
            // Всё ок, данные всех хранилищ выверены, собрать mreq и запустить экшен аплоада и проверки самого файла.
            LOGGER.trace(s"$logPrefix Allowed to process file upload, swfs=>${swfsOpt.orNull}")
            val mreq = MUploadReq(
              swfsOpt = swfsOpt,
              request = request0,
              user    = user
            )
            f(mreq)
          }
          .recover { case ex: Throwable =>
            // Рядом с текущим узлом нет искомой swfs volume. Это значит, что юзер подменил хостнейм в сгенеренной ссылке,
            // и пытается залить файл мимо целевого сервера (либо какая-то ошибка в конфигурации).
            LOGGER.warn(s"$logPrefix Failed to validate SWFS upload args", ex)
            Results.ExpectationFailed(s"Storage ${upTg.storage}:${upTg.storage.host.nameInt}:${upTg.storage.storageInfo} looks unavailable for upload from ${uploadUtil.MY_NODE_PUBLIC_URL}.")
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
    dab.async(action.parser) { request =>
      _apply(upTg, ctxIdOpt, request)(action.apply)
    }
  }

}
