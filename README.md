#  Placement + Internship Management System

A comprehensive Java-based placement and internship management system with MySQL database integration. This system helps manage student placements, internship applications, and company recruitment processes with intelligent matching algorithms.

## Features

### For Students
- User registration and profile management
- View eligible companies based on CGPA, branch, and skills
- Apply for job placements and internships
- Track application status (applied, shortlisted, offered, placed)
- View personalized recommendations

### For Admins
- Add companies and post internships
- Shortlist top-K candidates using priority queue algorithms
- Offer jobs and place students
- Accept interns and convert internships to full-time positions
- Generate placement and internship reports
- Process job applications using queue data structure

### For Companies
- Post job openings with specific requirements
- Set minimum CGPA, branch, and skill requirements
- Schedule visit dates

## Technology Stack

- **Language**: Java
- **Database**: MySQL
- **JDBC**: MySQL Connector/J
- **Data Structures**: Priority Queue, Custom Queue implementation

## Database Schema

The system uses 5 main tables:
- `users` - Student, admin, and company accounts
- `companies` - Company details and job requirements
- `applications` - Job application tracking
- `internships` - Internship postings
- `internship_applications` - Internship application tracking
