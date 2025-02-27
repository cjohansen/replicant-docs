locals {
  app_name = "replicant_docs"
  domain_name = "replicant.fun"
  hosted_zone = "replicant.fun."
}

module "site_bucket" {
  source = "./bucket"
  bucket_name = "replicant.fun"
  app_name = "${local.app_name}"
  domain_name = "${local.domain_name}"
  hosted_zone = "${local.hosted_zone}"
  providers = {
    aws = aws
    aws.us-east-1 = aws.us-east-1
  }
}

module "distribution" {
  source = "./distribution"
  app_name = "${local.app_name}"
  domain_name = "${local.domain_name}"
  hosted_zone = "${local.hosted_zone}"
  immutable_path = "/bundles/*"
  bucket_regional_domain_name = "${module.site_bucket.bucket_regional_domain_name}"
  cloudfront_access_identity_path = "${module.site_bucket.cloudfront_access_identity_path}"
  certificate_arn = "${module.site_bucket.certificate_arn}"
  providers = {
    aws = aws
    aws.us-east-1 = aws.us-east-1
  }
}
