# Non-secret settings. EDIT the two values marked CHANGE ME before running `terraform apply`.
aws_region    = "ap-south-1"
project_name  = "devops-calculator"
instance_type = "t3.micro"

key_name   = "my-ec2-key"      # CHANGE ME: an EC2 key pair that already exists in this region
admin_cidr = "203.0.113.10/32" # CHANGE ME: your public IP (find it with: curl ifconfig.me)
