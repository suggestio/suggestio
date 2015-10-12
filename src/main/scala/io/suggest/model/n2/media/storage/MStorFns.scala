package io.suggest.model.n2.media.storage

import io.suggest.common.menum.EnumValue2Val
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:57
 * Description: Имена полей для моделей [[IMediaStorage]].
 */
object MStorFns extends EnumValue2Val {

  protected[this] abstract class Val(val fn: String)
    extends super.Val(fn)
  {
    def esMappingProp: DocField
  }

  override type T = Val

  // common
  val STYPE       : T = new Val("t") {
    override def esMappingProp = FieldString(fn, FieldIndexingVariants.not_analyzed, include_in_all = false)
  }

  // cassandra
  val QUALIFIER   : T = new Val("q") {
    override def esMappingProp = FieldString(fn, FieldIndexingVariants.no, include_in_all = false)
  }
  val ROW_KEY     : T = new Val("u") {
    override def esMappingProp = FieldString(fn, FieldIndexingVariants.no, include_in_all = false)
  }

  // seaweedfs
  val FID         : T = new Val("f") {
    override def esMappingProp: DocField = {
      FieldObject("f", enabled = true, properties = Seq(
        FieldNumber(Fid.VOLUME_ID_FN, fieldType = DocFieldTypes.integer, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
        FieldString(Fid.FILE_ID_FN, index = FieldIndexingVariants.no, include_in_all = false)
      ))
    }
  }

}
