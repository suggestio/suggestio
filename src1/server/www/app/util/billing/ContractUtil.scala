package util.billing

import com.google.inject.Singleton
import io.suggest.mbill2.m.contract.MContract
import org.joda.time.DateTime
import play.api.data._, Forms._
import util.FormUtil.{localDateM, strTrimSanitizeF, strIdentityF, strFmtTrimF}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.15 12:23
 * Description: Утиль для работы с новыми контрактами в веб-морде: формы и всякие функции.
 */
@Singleton
class ContractUtil {

  /** Внутренний маппинг для даты LocalDate. */
  def bDate = localDateM
    .transform[DateTime](_.toDateTimeAtStartOfDay, _.toLocalDate)

  def suffixOptM: Mapping[Option[String]] = {
    optional(
      text(maxLength = 16)
        .transform(strTrimSanitizeF, strIdentityF)
    )
  }

  def hiddenInfoOptM: Mapping[Option[String]] = {
    text(maxLength = 4096)
      .transform(strFmtTrimF, strIdentityF)
      .transform[Option[String]](
        {Some(_).filter(!_.isEmpty)},
        {_ getOrElse ""}
      )
  }

  /** Маппинг для контракта. */
  def contractM: Mapping[MContract] = {
    mapping(
      "dateCreated"  -> bDate,
      "suffix"       -> suffixOptM,
      "hiddenInfo"   -> hiddenInfoOptM
    )
    {(dateCreated, suffix, hiddenInfo) =>
      MContract(
        dateCreated = dateCreated,
        suffix      = suffix,
        hiddenInfo  = hiddenInfo
      )
    }
    {mc =>
      Some((mc.dateCreated, mc.suffix, mc.hiddenInfo))
    }
  }

  /** Маппинг формы контракта. */
  def contractForm = Form(contractM)

}


/** Интерфейс для DI-поля с экземпляром [[ContractUtil]]. */
trait IContractUtilDi {
  def contractUtil: ContractUtil
}
