package controllers.pay

import controllers.SioControllerImpl
import io.suggest.bill.MPrice
import io.suggest.stat.m.{MAction, MActionTypes}
import io.suggest.util.Lists
import io.suggest.util.logs.MacroLogsImpl
import models.mctx.Context
import models.mpay.yaka.MYakaReq
import models.mproj.ICommonDi
import models.req.IReqHdr
import play.api.data.Form
import play.api.mvc.BodyParser
import util.acl.MaybeAuth
import util.pay.yaka.YakaUtil
import util.stat.StatUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.17 15:02
  * Description: Контроллер для ПС Яндекс.Касса, которая к сбербанку ближе.
  *
  * @see Прямая ссылка: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9D%D0%B0%D1%87%D0%B0%D0%BB%D0%BE-%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D0%B8]]
  *      Короткая ссылка: [[https://goo.gl/Zfwt15]]
  */
class PayYaka(
               maybeAuth                : MaybeAuth,
               statUtil                 : StatUtil,
               yakaUtil                 : YakaUtil,
               override val mCommonDi   : ICommonDi
             )
  extends SioControllerImpl
  with MacroLogsImpl
{

  import mCommonDi.{ec, errorHandler}

  /**
    * Логгировать, если неуклюжие ксакепы ковыряются в API сайта.
    * @param request Инстанс HTTP-реквеста.
    * @return Some(stat action) если есть странность с данными для MStat/kibana.
    *         None если всё ок.
    */
  private def _logIfHasCookies(implicit request: IReqHdr): Option[MAction] = {
    if (mCommonDi.isProd && request.cookies.nonEmpty) {
      LOGGER.error(s"_logIfHasCookies[${System.currentTimeMillis()}]: Unexpected cookies ${request.cookies} from client ${request.remoteAddress}, personId = ${request.user.personIdOpt}")
      val ma = MAction(
        actions = MActionTypes.UnexpectedCookies :: Nil,
        textNi  = {
          val hdrStr = request.headers
            .headers
            .iterator
            .map { case (k,v) => k + ": " + v }
            .mkString("\n")
          request.cookies.toString() :: hdrStr :: Nil
        }
      )
      Some(ma)

    } else {
      None
    }
  }


  /** Body-parser для обыденных HTTP-запросов яндекс-кассы. */
  private def _yakaReqBp: BodyParser[MYakaReq] = {
    parse.form(
      yakaUtil.md5Form,
      maxLength = Some(4096L),
      onErrors = { formWithErrors: Form[MYakaReq] =>
        LOGGER.error(s"Failed to bind yaka request: ${formatFormErrors(formWithErrors)}")
        BadRequest
      }
    )
  }


  /**
    * Экшен проверки платежа яндекс-кассой.
    *
    * @see Пример с CURL: [[https://github.com/yandex-money/yandex-money-joinup/blob/master/demo/010%20%D0%B8%D0%BD%D1%82%D0%B5%D0%B3%D1%80%D0%B0%D1%86%D0%B8%D1%8F%20%D0%B4%D0%BB%D1%8F%20%D1%81%D0%B0%D0%BC%D0%BE%D0%BF%D0%B8%D1%81%D0%BD%D1%8B%D1%85%20%D1%81%D0%B0%D0%B9%D1%82%D0%BE%D0%B2.md#%D0%9F%D1%80%D0%B8%D0%BC%D0%B5%D1%80-curl]]
    * @return 200 OK + XML, когда всё нормально.
    */
  def check = maybeAuth().async { implicit request =>
    lazy val logPrefix = s"check[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix ${request.remoteAddress} ${request.body}")

    // Надо забиндить тело запроса в форму.
    yakaUtil.md5Form.bindFromRequest().fold(
      {formWithErrors =>
        LOGGER.debug(s"$logPrefix Failed to bind yaka request: ${formatFormErrors(formWithErrors)}")
        // Скрываемся за страницей 404. Для снижения потока мусора от URL-сканнеров.
        errorHandler.http404Fut
      },

      {yReq =>

        // Реквест похож на денежный. Сначала можно просто пересчитать md5:
        val yPrice = MPrice(yReq.amount, yReq.currency)
        val expMd5 = yakaUtil.getMd5(yReq, yPrice)
        val res = if ( expMd5.equalsIgnoreCase(yReq.md5) ) {
          // Реквест послан от того, кто знает пароль. TODO Проверить данные в реквесте
          ???

        } else {
          LOGGER.error(s"$logPrefix yaka req binded, but md5 is invalid:\n $yReq\n calculated = $expMd5 , but provided = ${yReq.md5}")
          // TODO Записывать в статистику.
          errorHandler.http404Fut
        }

        // Запустить подготовку статистики. Тут Future[], но в данном случае этот код синхронный O(1).
        val userSaOptFut = statUtil.userSaOptFutFromRequest()

        val sa0Opt = _logIfHasCookies
        // Записать в статистику результат этой работы
        // Используем mstat для логгирования, чтобы всё стало видно в kibana.
        val ma1 = MAction(
          actions = MActionTypes.PayCheck :: Nil
        )
        implicit val ctx1 = implicitly[Context]
        userSaOptFut
          .flatMap { userSaOpt1 =>
            val s2 = new statUtil.Stat2 {
              override def statActions = {
                Lists.prependOpt(sa0Opt) {
                  ma1 :: Nil
                }
              }
              override def userSaOpt = userSaOpt1
              override def ctx = ctx1
            }
            val saveFut = statUtil.saveStat(s2)
            saveFut.onFailure { case ex: Throwable =>
              LOGGER.error(s"$logPrefix Unable to save stat $s2", ex)
            }
            saveFut
          }

        ???
      }
    )
  }

}
