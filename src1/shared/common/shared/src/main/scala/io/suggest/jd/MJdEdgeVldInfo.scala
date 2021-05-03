package io.suggest.jd

import io.suggest.common.geom.d2.ISize2di
import io.suggest.err.ErrorConstants
import io.suggest.img.MImgFormat
import io.suggest.n2.edge.MPredicates
import io.suggest.scalaz.StringValidationNel
import scalaz.Validation

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.17 17:36
  * Description: Cross-platform model for jd-edge validation.
  * Cross-platform used for ability to client-side validation debugging/using.
  */
object MJdEdgeVldInfo {

  /** JD-edge validate for to-other-ad pointer edge.
    * Implemented for jd-events.
    * @param m Additional validation info about current jd-edge.
    */
  def validateForAd(m: MJdEdgeVldInfo): StringValidationNel[MJdEdgeVldInfo] = {
    Validation
      .liftNel( m.jdEdge.predicate )(
        {pred =>
          !(pred eqOrHasParent MPredicates.JdContent.Ad)
        },
        "jd-pred " + ErrorConstants.Words.INVALID
      )
      .andThen { _ =>
        Validation.liftNel( m.jdEdge.nodeId )(_.isEmpty, "ad nodeId " + ErrorConstants.Words.MISSING)
      }
      .andThen { _ =>
        Validation.liftNel( m.file )(_.nonEmpty, "file " + ErrorConstants.Words.UNEXPECTED)
      }
      .map { _ =>
        m
      }
  }


  /** JD-edge validation for text edges. */
  def validateText(m: MJdEdgeVldInfo): StringValidationNel[MJdEdgeVldInfo] = {
    Validation
      .liftNel( m.jdEdge.predicate )(
        pred => !(pred eqOrHasParent MPredicates.JdContent.Text),
        MPredicates.JdContent.Text.toString + " " + ErrorConstants.Words.EXPECTED,
      )
      .andThen { _ =>
        Validation.liftNel( m.jdEdge.edgeDoc.text )(_.isEmpty, ErrorConstants.Words.MISSING)
      }
      .andThen { _ =>
        Validation.liftNel( m.file )( _.nonEmpty, ErrorConstants.Words.UNEXPECTED )
      }
      .andThen { _ =>
        Validation.liftNel( m.jdEdge.nodeId )(_.nonEmpty, ErrorConstants.Words.UNEXPECTED)
      }
      .map { _ =>
        m
      }
  }

}


/** Some collected info about for one jd-edge validation. */
final case class MJdEdgeVldInfo(
                                 jdEdge : MJdEdge,
                                 file   : Option[MJdEdgeFileVldInfo],
                               )



/** Jd-edge validation info data for related file (usually - image). */
final case class MJdEdgeFileVldInfo(
                                     isImg     : Boolean,
                                     imgWh     : Option[ISize2di],
                                     dynFmt    : Option[MImgFormat],
                                   )

