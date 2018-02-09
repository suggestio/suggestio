package io.suggest.img

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.playx.FormMappingUtil
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.02.18 10:56
  * Description: JVM-only утиль для common-модели
  */
object MImgFmtJvm {

  implicit def outImgFmtQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MImgFmt] = {
    EnumeratumJvmUtil.valueEnumQsb( MImgFmts )
  }

  def mappingOpt = EnumeratumJvmUtil.stringIdOptMapping( MImgFmts )
  def mapping = FormMappingUtil.optMapping2required( mappingOpt )

}
