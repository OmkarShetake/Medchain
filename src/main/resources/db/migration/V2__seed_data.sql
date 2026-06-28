-- Insert admin user (password: Admin@123)
INSERT INTO users (id, name, email, password, role, is_verified, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Admin User',
    'admin@medchain.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.yvrcEO',
    'ADMIN',
    true,
    true
);

-- Insert manufacturer user (password: Mfr@123)
INSERT INTO users (id, name, email, password, role, phone, is_verified, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'Cipla Pharmaceuticals',
    'manufacturer@medchain.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.yvrcEO',
    'MANUFACTURER',
    '+919876543210',
    true,
    true
);

-- Insert patient user (password: Patient@123)
INSERT INTO users (id, name, email, password, role, phone, is_verified, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'Rahul Sharma',
    'patient@medchain.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.yvrcEO',
    'PATIENT',
    '+919876543211',
    true,
    true
);

-- Insert chemist user (password: Chemist@123)
INSERT INTO users (id, name, email, password, role, phone, is_verified, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000004',
    'Apollo Pharmacy',
    'chemist@medchain.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYIR.yvrcEO',
    'CHEMIST',
    '+919876543212',
    true,
    true
);

-- Insert manufacturer details
INSERT INTO manufacturers (id, user_id, company_name, license_number, gst_number, is_verified, address, city, state, pincode)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000002',
    'Cipla Pharmaceuticals Ltd',
    'MH-MFG-2024-001',
    '27AAACR5055K1Z4',
    true,
    'Cipla House, Peninsula Business Park, Ganpatrao Kadam Marg',
    'Mumbai',
    'Maharashtra',
    '400013'
);

-- Insert sample medicines
INSERT INTO medicines (id, manufacturer_id, name, generic_name, composition, category, batch_number, manufacturing_date, expiry_date, storage_instructions, quantity_produced, status, description, side_effects)
VALUES
(
    '00000000-0000-0000-0000-000000000020',
    '00000000-0000-0000-0000-000000000010',
    'Dolo 650',
    'Paracetamol',
    'Paracetamol 650mg',
    'PAINKILLER',
    'DOLO-2024-001',
    '2024-01-01',
    '2026-01-01',
    'Store in a cool, dry place away from direct sunlight',
    10000,
    'ACTIVE',
    'Used for fever and mild to moderate pain relief',
    'Nausea, skin rash, allergic reactions in rare cases'
),
(
    '00000000-0000-0000-0000-000000000021',
    '00000000-0000-0000-0000-000000000010',
    'Azithromycin 500',
    'Azithromycin',
    'Azithromycin 500mg',
    'ANTIBIOTIC',
    'AZITH-2024-001',
    '2024-02-01',
    '2026-02-01',
    'Store below 25°C, protect from moisture',
    5000,
    'ACTIVE',
    'Antibiotic used to treat bacterial infections',
    'Diarrhea, nausea, abdominal pain, vomiting'
),
(
    '00000000-0000-0000-0000-000000000022',
    '00000000-0000-0000-0000-000000000010',
    'Cetirizine 10mg',
    'Cetirizine',
    'Cetirizine Hydrochloride 10mg',
    'ANTIHISTAMINE',
    'CETIR-2024-001',
    '2024-03-01',
    '2026-03-01',
    'Store at room temperature',
    8000,
    'ACTIVE',
    'Used for allergic rhinitis and urticaria',
    'Drowsiness, dry mouth, fatigue'
),
(
    '00000000-0000-0000-0000-000000000023',
    '00000000-0000-0000-0000-000000000010',
    'Vitamin D3 60K',
    'Cholecalciferol',
    'Cholecalciferol 60000 IU',
    'VITAMIN',
    'VITD-2024-001',
    '2024-04-01',
    '2026-04-01',
    'Store in a cool, dry place',
    15000,
    'ACTIVE',
    'Vitamin D3 supplement for bone health',
    'Rare: nausea, constipation, weakness'
),
(
    '00000000-0000-0000-0000-000000000024',
    '00000000-0000-0000-0000-000000000010',
    'Omeprazole 20mg',
    'Omeprazole',
    'Omeprazole 20mg',
    'ANTACID',
    'OMEP-2024-001',
    '2024-05-01',
    '2026-05-01',
    'Store below 30°C, protect from light',
    12000,
    'ACTIVE',
    'Proton pump inhibitor for acid reflux and ulcers',
    'Headache, diarrhea, stomach pain'
);

-- Insert sample medicine units with QR codes
INSERT INTO medicine_units (medicine_id, qr_code, strip_number, status, distributed_state, distributed_city, distributed_at)
VALUES
('00000000-0000-0000-0000-000000000020', 'MEDCHAIN:550e8400-e29b-41d4-a716-446655440001', 'STRIP-001', 'ACTIVE', 'Maharashtra', 'Mumbai', NOW()),
('00000000-0000-0000-0000-000000000020', 'MEDCHAIN:550e8400-e29b-41d4-a716-446655440002', 'STRIP-002', 'ACTIVE', 'Maharashtra', 'Pune', NOW()),
('00000000-0000-0000-0000-000000000021', 'MEDCHAIN:550e8400-e29b-41d4-a716-446655440003', 'STRIP-001', 'ACTIVE', 'Karnataka', 'Bangalore', NOW()),
('00000000-0000-0000-0000-000000000022', 'MEDCHAIN:550e8400-e29b-41d4-a716-446655440004', 'STRIP-001', 'ACTIVE', 'Delhi', 'New Delhi', NOW()),
('00000000-0000-0000-0000-000000000023', 'MEDCHAIN:550e8400-e29b-41d4-a716-446655440005', 'STRIP-001', 'ACTIVE', 'Tamil Nadu', 'Chennai', NOW());
