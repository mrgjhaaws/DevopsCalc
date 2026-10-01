output "vpc_id" {
  description = "ID of the VPC"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "ID of the public subnet"
  value       = aws_subnet.public.id
}

output "ecr_repository_url" {
  description = "Push Docker images here"
  value       = aws_ecr_repository.app.repository_url
}

output "ec2_instance_id" {
  description = "ID of the EC2 instance"
  value       = aws_instance.app.id
}

output "ec2_public_ip" {
  description = "Public IP of the server (put this in ansible/inventory.ini)"
  value       = aws_instance.app.public_ip
}

output "app_url" {
  description = "URL of the calculator once Ansible has deployed it"
  value       = "http://${aws_instance.app.public_ip}:${var.app_port}"
}
