## 脚本命令行
bird训练集
python run_eval.py --dataset audit_natural_resource --mode local-db --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass zbf103056


python run_eval.py --dataset bird --api http://localhost:8080 --db-config-id 1 --username test --password zbf103056  --limit 5
python run_eval.py --dataset bird --api http://localhost:8080 --db-config-id 1 --username test --password zbf103056 --db-pass zbf103056 --init-schema --limit 5

python run_eval.py --dataset bird --api http://localhost:8080 --db-host 127.0.0.1 --db-port 3306 --db-user root --db-pass zbf103056 --limit 5
