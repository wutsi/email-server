CREATE TABLE T_UNSUBSCRIBED(
  id                      SERIAL NOT NULL,

  site_id                 BIGINT NOT NULL,
  user_id                 BIGINT,
  email                   VARCHAR(255),
  unsubscribed_date_time  TIMESTAMPTZ NOT NULL DEFAULT now(),

  UNIQUE(site_id, user_id, email),
  PRIMARY KEY(id)
);
