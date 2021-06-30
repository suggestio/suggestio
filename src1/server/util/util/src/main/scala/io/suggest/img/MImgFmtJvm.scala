package io.suggest.img

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import io.suggest.xplay.qsb.CrossQsBindable
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 10:56
  * Description: JVM-only утиль для common-модели
  */
object MImgFmtJvm {

  implicit def outImgFmtQsb(implicit strB: QueryStringBindable[String]): CrossQsBindable[MImgFormat] = {
    EnumeratumJvmUtil.valueEnumQsb( MImgFormats )
  }

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( MImgFormats )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}
