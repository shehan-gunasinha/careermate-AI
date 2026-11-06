-- Demo data. ALL listings below are fictional SAMPLE jobs, not real vacancies.

INSERT INTO jobs (title, company_name, company_description, location, industry, employment_type, work_arrangement, experience_level, salary_min, salary_max, salary_currency, short_description, description, responsibilities, required_qualifications, preferred_qualifications, posted_date, deadline, application_url, source, external_id, fingerprint, demo, active, last_synced_at) VALUES
('Junior Software Engineer', 'Sample Tech Solutions (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Information Technology', 'FULL_TIME', 'HYBRID', 'JUNIOR', 18000, 30000, 'USD', 'Build and maintain Java and Spring Boot services in an agile team.', '[DEMO LISTING - not a real vacancy] Join a small product team building REST APIs and web features. You will write clean, tested Java code, review pull requests and learn modern cloud deployment practices from senior engineers.', 'Develop and maintain REST APIs using Java and Spring Boot
Write unit and integration tests
Participate in code reviews and sprint planning
Work with SQL databases and Git workflows', 'Bachelor''s degree in Computer Science, Software Engineering or a related field
0-2 years of professional or internship experience
Good knowledge of Java and object-oriented design
Understanding of SQL and REST APIs', 'Experience with Docker and basic cloud deployment (AWS)
Exposure to Kubernetes fundamentals
Familiarity with React or another frontend framework', current_date - 3, current_date + 30, NULL, 'DEMO', 'demo-001', encode(sha256(convert_to(lower('Junior Software Engineer' || '|' || 'Sample Tech Solutions (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Business Analyst', 'Sample Consulting Group (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Business Management', 'FULL_TIME', 'HYBRID', 'MID', 24000, 38000, 'USD', 'Translate business needs into clear requirements and improve processes.', '[DEMO LISTING - not a real vacancy] Work with stakeholders to gather requirements, analyse processes and support delivery teams. Strong communication and analytical skills are essential.', 'Gather and document business requirements
Analyse data and create reports
Facilitate workshops with stakeholders
Support user acceptance testing', 'Bachelor''s degree in Business, IT or a related field
3+ years in a business analysis or similar role
Strong written and verbal communication', 'Certification in business analysis
Experience with Power BI or Tableau', current_date - 5, current_date + 25, NULL, 'DEMO', 'demo-002', encode(sha256(convert_to(lower('Business Analyst' || '|' || 'Sample Consulting Group (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Accountant', 'Sample Finance Partners (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Kandy, Sri Lanka', 'Finance and Accounting', 'FULL_TIME', 'ON_SITE', 'MID', 15000, 26000, 'USD', 'Manage ledgers, month-end close and financial reporting.', '[DEMO LISTING - not a real vacancy] Support the finance team with day-to-day accounting, reconciliations, tax filings and monthly reporting.', 'Maintain general ledger and reconcile accounts
Prepare monthly and annual financial statements
Support audits and tax filings
Manage accounts payable and receivable', 'Bachelor''s degree in Accounting or Finance, or a recognised professional accounting qualification (part-qualified acceptable)
2+ years of accounting experience
Knowledge of IFRS', 'Experience with QuickBooks or SAP
Payroll experience', current_date - 2, current_date + 21, NULL, 'DEMO', 'demo-003', encode(sha256(convert_to(lower('Accountant' || '|' || 'Sample Finance Partners (Demo)' || '|' || 'Kandy, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Marketing Executive', 'Sample Brand Studio (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Marketing', 'FULL_TIME', 'HYBRID', 'JUNIOR', 12000, 20000, 'USD', 'Plan and run digital campaigns across social, email and search.', '[DEMO LISTING - not a real vacancy] Help plan, execute and measure multi-channel marketing campaigns and create engaging content for a growing consumer brand.', 'Execute social media and email campaigns
Write and schedule content
Track campaign performance in analytics tools
Support events and brand activities', 'Diploma or degree in Marketing, Communications or a related field
1-2 years of marketing experience
Strong writing skills', 'Experience with Google Ads and Google Analytics
Basic design skills (Canva)', current_date - 1, current_date + 28, NULL, 'DEMO', 'demo-004', encode(sha256(convert_to(lower('Marketing Executive' || '|' || 'Sample Brand Studio (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Customer Support Executive', 'Sample Connect Services (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Customer Service', 'FULL_TIME', 'ON_SITE', 'ENTRY', 6000, 10000, 'USD', 'Help customers by phone, chat and email and resolve issues quickly.', '[DEMO LISTING - not a real vacancy] Be the first point of contact for customers. Full training provided; shift work may apply.', 'Respond to customer enquiries via phone, chat and email
Log and track issues in the ticketing system
Escalate complex cases
Maintain customer satisfaction targets', 'GCE Advanced Level or equivalent
Excellent communication skills in English
Patience and problem-solving mindset', 'Previous customer service experience
Familiarity with ticketing systems or CRM', current_date - 4, current_date + 14, NULL, 'DEMO', 'demo-005', encode(sha256(convert_to(lower('Customer Support Executive' || '|' || 'Sample Connect Services (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Mechanical Engineer', 'Sample Industrial Works (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Galle, Sri Lanka', 'Engineering', 'FULL_TIME', 'ON_SITE', 'MID', 20000, 34000, 'USD', 'Design and improve mechanical systems for manufacturing equipment.', '[DEMO LISTING - not a real vacancy] Take part in the design, testing and maintenance of mechanical systems in a manufacturing environment.', 'Create and review mechanical designs in CAD
Support installation, testing and maintenance
Improve manufacturing processes
Prepare technical documentation', 'Bachelor''s degree in Mechanical Engineering
3+ years of relevant experience
Proficiency in CAD tools', 'Chartered engineer membership (or progress towards it)
Lean Six Sigma knowledge', current_date - 7, current_date + 40, NULL, 'DEMO', 'demo-006', encode(sha256(convert_to(lower('Mechanical Engineer' || '|' || 'Sample Industrial Works (Demo)' || '|' || 'Galle, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('HR Assistant', 'Sample People Co (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Human Resources', 'FULL_TIME', 'ON_SITE', 'ENTRY', 8000, 13000, 'USD', 'Support recruitment, onboarding and HR administration.', '[DEMO LISTING - not a real vacancy] Assist the HR team with recruitment coordination, onboarding, employee records and general HR administration.', 'Schedule interviews and coordinate candidates
Prepare onboarding materials
Maintain employee records
Answer routine HR queries', 'Diploma or degree in HR Management, Business or a related field
Strong organisational and communication skills
Proficiency in Microsoft Office', 'Internship or experience in HR
Knowledge of labour regulations', current_date - 6, current_date + 20, NULL, 'DEMO', 'demo-007', encode(sha256(convert_to(lower('HR Assistant' || '|' || 'Sample People Co (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Graphic Designer', 'Sample Creative Lab (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Remote', 'Design', 'CONTRACT', 'REMOTE', 'MID', 15000, 28000, 'USD', 'Create brand visuals, social graphics and marketing collateral.', '[DEMO LISTING - not a real vacancy] Work with the creative director to produce brand identities, print and digital assets for clients across industries.', 'Design logos, social media graphics and print materials
Prepare files for production
Incorporate client and team feedback
Maintain brand consistency', 'Portfolio demonstrating branding and layout work
3+ years of design experience
Expertise in Adobe Creative Suite', 'Motion graphics or video editing skills
UI/UX design exposure', current_date - 2, current_date + 35, NULL, 'DEMO', 'demo-008', encode(sha256(convert_to(lower('Graphic Designer' || '|' || 'Sample Creative Lab (Demo)' || '|' || 'Remote'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Primary School Teacher', 'Sample Learning Academy (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Negombo, Sri Lanka', 'Education', 'FULL_TIME', 'ON_SITE', 'JUNIOR', 7000, 12000, 'USD', 'Teach and support primary-age students in a supportive school.', '[DEMO LISTING - not a real vacancy] Plan and deliver engaging lessons, assess student progress and communicate with parents.', 'Plan and teach lessons
Assess and report on student progress
Manage classroom behaviour positively
Communicate with parents', 'Degree or diploma in Education or a relevant field
Teaching qualification
Patience and strong communication skills', 'Experience with primary-age students
First aid certificate', current_date - 8, current_date + 45, NULL, 'DEMO', 'demo-009', encode(sha256(convert_to(lower('Primary School Teacher' || '|' || 'Sample Learning Academy (Demo)' || '|' || 'Negombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Front Desk Associate', 'Sample Harbour Hotel (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Colombo, Sri Lanka', 'Hospitality', 'PART_TIME', 'ON_SITE', 'ENTRY', 4000, 7000, 'USD', 'Welcome guests, manage check-ins and handle reservations.', '[DEMO LISTING - not a real vacancy] Be the welcoming face of the hotel, handling check-in and check-out, reservations and guest requests.', 'Greet guests and manage check-in/out
Handle reservations and enquiries
Coordinate with housekeeping and other departments
Resolve guest concerns politely', 'Good communication skills in English
Willingness to work shifts
Basic computer skills', 'Previous hospitality experience
Additional language skills', current_date - 9, current_date + 18, NULL, 'DEMO', 'demo-010', encode(sha256(convert_to(lower('Front Desk Associate' || '|' || 'Sample Harbour Hotel (Demo)' || '|' || 'Colombo, Sri Lanka'), 'UTF8')), 'hex'), TRUE, TRUE, now()),
('Data Analyst Intern', 'Sample Insights Lab (Demo)', 'Fictional company used only for CareerMate AI demonstration data.', 'Remote', 'Information Technology', 'INTERNSHIP', 'REMOTE', 'ENTRY', NULL, NULL, NULL, 'Learn data analysis on real reporting projects with mentor support.', '[DEMO LISTING - not a real vacancy] A 6-month internship supporting analysts with data cleaning, dashboards and reporting.', 'Clean and prepare datasets
Build simple dashboards
Support reporting for internal teams
Document findings', 'Currently studying or recently completed a degree in a quantitative field
Basic SQL and spreadsheet skills', 'Python or R basics
Experience with a BI tool', current_date - 1, current_date + 22, NULL, 'DEMO', 'demo-011', encode(sha256(convert_to(lower('Data Analyst Intern' || '|' || 'Sample Insights Lab (Demo)' || '|' || 'Remote'), 'UTF8')), 'hex'), TRUE, TRUE, now());

INSERT INTO job_skills (job_id, skill, required)
SELECT j.id, s.skill, s.required FROM jobs j JOIN (VALUES
('demo-001', 'Java', TRUE),
('demo-001', 'Spring Boot', TRUE),
('demo-001', 'SQL', TRUE),
('demo-001', 'REST APIs', TRUE),
('demo-001', 'Git', TRUE),
('demo-001', 'Docker', FALSE),
('demo-001', 'AWS', FALSE),
('demo-001', 'Kubernetes', FALSE),
('demo-002', 'Business Analysis', TRUE),
('demo-002', 'Requirements Gathering', TRUE),
('demo-002', 'Stakeholder Management', TRUE),
('demo-002', 'Excel', TRUE),
('demo-002', 'Communication', TRUE),
('demo-002', 'Power BI', FALSE),
('demo-002', 'SQL', FALSE),
('demo-002', 'Process Improvement', FALSE),
('demo-003', 'Accounting', TRUE),
('demo-003', 'Financial Reporting', TRUE),
('demo-003', 'IFRS', TRUE),
('demo-003', 'Excel', TRUE),
('demo-003', 'Accounts Payable', TRUE),
('demo-003', 'QuickBooks', FALSE),
('demo-003', 'SAP', FALSE),
('demo-003', 'Taxation', FALSE),
('demo-004', 'Digital Marketing', TRUE),
('demo-004', 'Social Media', TRUE),
('demo-004', 'Content Marketing', TRUE),
('demo-004', 'Copywriting', TRUE),
('demo-004', 'Google Analytics', FALSE),
('demo-004', 'SEO', FALSE),
('demo-004', 'Canva', FALSE),
('demo-004', 'Email Marketing', FALSE),
('demo-005', 'Customer Support', TRUE),
('demo-005', 'Communication', TRUE),
('demo-005', 'Problem Solving', TRUE),
('demo-005', 'Ticketing Systems', FALSE),
('demo-005', 'CRM', FALSE),
('demo-005', 'Conflict Resolution', FALSE),
('demo-006', 'Mechanical Design', TRUE),
('demo-006', 'AutoCAD', TRUE),
('demo-006', 'SolidWorks', TRUE),
('demo-006', 'Manufacturing', TRUE),
('demo-006', 'Project Management', TRUE),
('demo-006', 'Lean Six Sigma', FALSE),
('demo-006', 'Maintenance', FALSE),
('demo-006', 'Thermodynamics', FALSE),
('demo-007', 'HR Administration', TRUE),
('demo-007', 'Recruitment', TRUE),
('demo-007', 'Microsoft Office', TRUE),
('demo-007', 'Communication', TRUE),
('demo-007', 'Onboarding', FALSE),
('demo-007', 'Employee Relations', FALSE),
('demo-007', 'Scheduling', FALSE),
('demo-008', 'Graphic Design', TRUE),
('demo-008', 'Adobe Photoshop', TRUE),
('demo-008', 'Adobe Illustrator', TRUE),
('demo-008', 'Typography', TRUE),
('demo-008', 'Branding', TRUE),
('demo-008', 'Adobe InDesign', FALSE),
('demo-008', 'Video Editing', FALSE),
('demo-008', 'Figma', FALSE),
('demo-009', 'Teaching', TRUE),
('demo-009', 'Lesson Planning', TRUE),
('demo-009', 'Classroom Management', TRUE),
('demo-009', 'Communication', TRUE),
('demo-009', 'Curriculum Development', FALSE),
('demo-009', 'First Aid', FALSE),
('demo-009', 'Tutoring', FALSE),
('demo-010', 'Front Desk', TRUE),
('demo-010', 'Customer Service', TRUE),
('demo-010', 'Communication', TRUE),
('demo-010', 'Conflict Resolution', FALSE),
('demo-010', 'Microsoft Office', FALSE),
('demo-011', 'SQL', TRUE),
('demo-011', 'Excel', TRUE),
('demo-011', 'Data Analysis', TRUE),
('demo-011', 'Python', FALSE),
('demo-011', 'Power BI', FALSE),
('demo-011', 'Tableau', FALSE)
) AS s(ext, skill, required) ON j.external_id = s.ext AND j.source = 'DEMO';
