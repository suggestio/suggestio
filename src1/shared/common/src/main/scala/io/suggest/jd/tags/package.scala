package io.suggest.jd

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.17 12:54
  */
package object tags {

  type JdBatchF_t  = Seq[IDocTag] => Seq[IDocTag]

  type JdBatch_t   = (IDocTag, JdBatchF_t)

  type JdBatches_t = Seq[JdBatch_t]

}
