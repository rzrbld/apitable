## Patch against apitable v1.0.0 beta

- clone original repo `git clone https://github.com/apitable/apitable.git`
- chekout v1.0.0 beta `git checkout a1f76e8555db0635ad7b8358a45a1d2c5d388fe0`
- patch with `cd apitable(original)/ && patch -s -p0 < apitable_v1.0.0_beta_ke.patch`

now you good to go with `make build-docker` and `docker-compose -f docker-compose.yaml up`