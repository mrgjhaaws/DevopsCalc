variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "ap-south-1"
}

variable "project_name" {
  description = "Name prefix for all resources (also the ECR repository name)"
  type        = string
  default     = "devops-calculator"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet"
  type        = string
  default     = "10.0.1.0/24"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.micro"
}

variable "key_name" {
  description = "Name of an existing EC2 key pair used for SSH (Ansible needs it)"
  type        = string
}

variable "admin_cidr" {
  description = "Your public IP in CIDR form, for example 203.0.113.10/32. Open-to-the-world SSH is rejected."
  type        = string

  validation {
    condition     = can(cidrhost(var.admin_cidr, 0)) && var.admin_cidr != "0.0.0.0/0"
    error_message = "admin_cidr must be a valid CIDR block and must not be 0.0.0.0/0."
  }
}

variable "app_port" {
  description = "Port the calculator listens on"
  type        = number
  default     = 8080
}
