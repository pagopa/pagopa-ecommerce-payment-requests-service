variable "env" {
  type        = string
  description = "Environment"
}

variable "github" {
  type = object({
    org        = string
    repository = string
  })
  description = "GitHub Organization and repository name"
  default = {
    org        = "pagopa"
    repository = "pagopa-ecommerce-payment-requests-service"
  }
}