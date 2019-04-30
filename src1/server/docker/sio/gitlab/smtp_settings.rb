if Rails.env.production?
  Rails.application.config.action_mailer.delivery_method = :smtp

  ActionMailer::Base.delivery_method = :smtp
  ActionMailer::Base.smtp_settings = {
    address: "192.168.101.9",
    port: 25,
    user_name: "no-reply@suggest.io",
    password: "BedCenEnakJidLilfEp",
    domain: "suggest.io",
    authentication: :login,
    enable_starttls_auto: false,
    openssl_verify_mode: 'peer' # See ActionMailer documentation for other possible options
  }
end
