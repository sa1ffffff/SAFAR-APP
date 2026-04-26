from docx import Document
from docx.shared import Pt, RGBColor, Inches
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

def add_colored_heading(doc, text, level, color_hex='#0A0A0A'):
    """Add a heading with custom color"""
    heading = doc.add_heading(text, level=level)
    for run in heading.runs:
        run.font.color.rgb = RGBColor(int(color_hex[1:3], 16), int(color_hex[3:5], 16), int(color_hex[5:7], 16))
    return heading

def add_bullet_point(doc, text):
    """Add a bullet point"""
    p = doc.add_paragraph(text, style='List Bullet')
    p.paragraph_format.left_indent = Inches(0.5)
    return p

def add_highlight_box(doc, text):
    """Add a highlighted text box"""
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Inches(0.5)
    p.paragraph_format.right_indent = Inches(0.5)
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(12)
    
    # Add shading
    shading_elm = OxmlElement('w:shd')
    shading_elm.set(qn('w:fill'), 'FFF9E6')
    p._element.get_or_add_pPr().append(shading_elm)
    
    run = p.add_run(text)
    run.font.size = Pt(11)
    return p

# Create document
doc = Document()

# Set default font
style = doc.styles['Normal']
style.font.name = 'Calibri'
style.font.size = Pt(11)

# Title
title = doc.add_heading('🚗 SAFAR', 0)
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
for run in title.runs:
    run.font.size = Pt(28)
    run.font.color.rgb = RGBColor(10, 10, 10)

# Urdu subtitle
urdu = doc.add_paragraph('سفر')
urdu.alignment = WD_ALIGN_PARAGRAPH.CENTER
urdu.runs[0].font.size = Pt(18)
urdu.runs[0].font.color.rgb = RGBColor(255, 214, 0)
urdu.runs[0].bold = True

# Subtitle
subtitle = doc.add_paragraph('Community Carpool Marketplace')
subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
subtitle.runs[0].font.size = Pt(14)
subtitle.runs[0].font.color.rgb = RGBColor(102, 102, 102)
subtitle.runs[0].italic = True

subtitle2 = doc.add_paragraph('Android Application - Project Proposal')
subtitle2.alignment = WD_ALIGN_PARAGRAPH.CENTER
subtitle2.runs[0].font.size = Pt(12)
subtitle2.runs[0].font.color.rgb = RGBColor(102, 102, 102)

doc.add_paragraph()

# 1. Problem Statement
add_colored_heading(doc, '1. Problem Statement', 1)
doc.add_paragraph(
    'Transportation in Pakistan, particularly for intercity travel, faces significant challenges that affect millions of commuters daily:'
)

problems = [
    'High Transportation Costs: Individual car ownership and taxi services are expensive for long-distance travel. Public transport, while affordable, often lacks comfort and reliability.',
    'Empty Seats, Wasted Resources: Thousands of private vehicles travel between major cities (Lahore, Islamabad, Karachi, etc.) with multiple empty seats. This represents wasted fuel, increased carbon emissions, and missed opportunities for cost sharing.',
    'Lack of Trusted Ride-Sharing Platforms: While international ride-sharing platforms exist, they are not optimized for Pakistan\'s unique intercity carpooling needs. There is no dedicated, community-driven platform that connects drivers and passengers heading the same direction.',
    'Safety and Trust Concerns: Informal carpooling arrangements lack proper verification, rating systems, and safety mechanisms, making users hesitant to share rides with strangers.',
    'Communication Barriers: Even when drivers and passengers find each other through social media groups, coordination is fragmented across multiple platforms with no centralized communication or booking system.'
]

for problem in problems:
    add_bullet_point(doc, problem)

add_highlight_box(doc, 'Core Problem: There is no simple, safe, and community-driven platform in Pakistan that allows drivers to monetize empty seats while providing affordable, flexible transportation options to passengers traveling the same route.')

# 2. Proposed Solution
add_colored_heading(doc, '2. Proposed Solution', 1)
doc.add_paragraph(
    'SAFAR (سفر — "Journey" in Urdu) is a peer-to-peer ride-sharing Android application designed specifically for Pakistan\'s intercity travel market. It connects drivers with available seats to passengers heading in the same direction, creating a community-driven carpool marketplace.'
)

doc.add_paragraph('Key Features:').runs[0].bold = True

features = [
    '🔐 Secure Authentication: Firebase Phone OTP authentication ensures verified users, reducing fraud and increasing trust.',
    '🚗 Post Ride Offers: Drivers can create ride ads specifying origin, destination, departure time, available seats, and price per seat.',
    '🗺️ Map-Based Discovery: Passengers can discover available rides through an interactive Google Maps interface with custom yellow pin markers.',
    '📋 List View Search: Alternative list-based browsing with search and filter options.',
    '📩 Request System: Passengers send join requests to drivers, who can accept or reject. Seat availability is managed transactionally to prevent overbooking.',
    '💬 Real-Time Chat: Firebase-powered in-app chat system for coordination after request acceptance.',
    '🔔 Push Notifications: Firebase Cloud Messaging keeps users informed about requests, acceptances, and messages.',
    '⭐ Rating & Reporting: Post-ride rating system and user reporting mechanism to maintain community standards.',
    '👤 User Profiles: View profiles with ratings, total rides given/taken, and profile pictures.'
]

for feature in features:
    add_bullet_point(doc, feature)

doc.add_paragraph('How It Works - For Drivers:').runs[0].bold = True
driver_steps = [
    'Create a ride ad with route, date/time, available seats, and price',
    'Receive join requests from interested passengers',
    'Accept or reject requests based on preferences',
    'Chat with confirmed passengers for pickup coordination',
    'Complete the ride and receive ratings'
]
for step in driver_steps:
    p = doc.add_paragraph(step, style='List Number')
    p.paragraph_format.left_indent = Inches(0.5)

doc.add_paragraph('How It Works - For Passengers:').runs[0].bold = True
passenger_steps = [
    'Browse available rides on map or list view',
    'Send join requests for desired rides',
    'Wait for driver approval',
    'Chat with driver upon acceptance to finalize pickup details',
    'Complete the ride and rate the experience'
]
for step in passenger_steps:
    p = doc.add_paragraph(step, style='List Number')
    p.paragraph_format.left_indent = Inches(0.5)

add_highlight_box(doc, 'Note: SAFAR focuses on coordination and community trust, not payment processing. Users arrange payment offline (cash, bank transfer, etc.), keeping the platform simple and accessible.')

# Page break
doc.add_page_break()

# 3. Technology Stack
add_colored_heading(doc, '3. Technology Stack', 1)

doc.add_paragraph('3.1 Development Environment').runs[0].bold = True

# Table for dev environment
table = doc.add_table(rows=9, cols=2)
table.style = 'Light Grid Accent 1'
table.autofit = False
table.allow_autofit = False

# Header row
hdr_cells = table.rows[0].cells
hdr_cells[0].text = 'Component'
hdr_cells[1].text = 'Specification'
for cell in hdr_cells:
    for paragraph in cell.paragraphs:
        for run in paragraph.runs:
            run.font.bold = True
            run.font.color.rgb = RGBColor(255, 214, 0)
    shading_elm = OxmlElement('w:shd')
    shading_elm.set(qn('w:fill'), '0A0A0A')
    cell._element.get_or_add_tcPr().append(shading_elm)

# Data rows
dev_data = [
    ['IDE', 'Android Studio (Latest Stable Version)'],
    ['Primary Language', 'Java'],
    ['Secondary Language', 'Kotlin (for UI components)'],
    ['Build System', 'Gradle 8.7.0'],
    ['Minimum SDK', 'API 24 (Android 7.0)'],
    ['Target SDK', 'API 35 (Android 15)'],
    ['Java Version', 'Java 11'],
    ['Kotlin Version', '2.0.21']
]

for i, (comp, spec) in enumerate(dev_data, start=1):
    row = table.rows[i]
    row.cells[0].text = comp
    row.cells[1].text = spec
    for cell in row.cells:
        for paragraph in cell.paragraphs:
            for run in paragraph.runs:
                run.font.size = Pt(10)

doc.add_paragraph()

doc.add_paragraph('3.2 Core Technologies').runs[0].bold = True

tech_list = [
    'Backend (Firebase): Auth (Phone OTP), Firestore (data storage), Realtime Database (chat), FCM (push notifications), Storage (images)',
    'UI Framework: Material Design 3, Jetpack Compose, XML layouts',
    'Maps & Location: Google Maps SDK, Maps Compose, Location Services',
    'Image Loading: Glide 4.16.0, Coil Compose 2.5.0',
    'Architecture: MVP (Model-View-Presenter) pattern for clean code separation'
]

for tech in tech_list:
    add_bullet_point(doc, tech)

# 4. Tools & Requirements
add_colored_heading(doc, '4. Tools & Requirements', 1)

doc.add_paragraph('4.1 Hardware Requirements').runs[0].bold = True

# Hardware table
hw_table = doc.add_table(rows=3, cols=3)
hw_table.style = 'Light Grid Accent 1'

# Header
hw_hdr = hw_table.rows[0].cells
hw_hdr[0].text = 'Item'
hw_hdr[1].text = 'Purpose'
hw_hdr[2].text = 'Specification'
for cell in hw_hdr:
    for paragraph in cell.paragraphs:
        for run in paragraph.runs:
            run.font.bold = True
            run.font.color.rgb = RGBColor(255, 214, 0)
    shading_elm = OxmlElement('w:shd')
    shading_elm.set(qn('w:fill'), '0A0A0A')
    cell._element.get_or_add_tcPr().append(shading_elm)

# Data
hw_data = [
    ['Development Computer', 'Running Android Studio', '• 8GB RAM (16GB recommended)\n• Intel i5/AMD Ryzen 5+\n• 20GB free space (SSD)\n• Windows/macOS/Linux'],
    ['Android Debug Phone', 'Real device testing', '• Android 7.0+\n• GPS enabled\n• Camera\n• USB debugging enabled']
]

for i, (item, purpose, spec) in enumerate(hw_data, start=1):
    row = hw_table.rows[i]
    row.cells[0].text = item
    row.cells[1].text = purpose
    row.cells[2].text = spec
    for cell in row.cells:
        for paragraph in cell.paragraphs:
            for run in paragraph.runs:
                run.font.size = Pt(9)

doc.add_paragraph()

doc.add_paragraph('Why a Physical Debugging Phone is Essential:').runs[0].bold = True

debug_reasons = [
    'GPS Testing: Emulators cannot simulate real-world GPS behavior and movement patterns',
    'Firebase Phone Auth: OTP verification requires actual SMS reception',
    'Push Notifications: FCM testing needs real device foreground/background behavior',
    'Camera Integration: Profile picture uploads require functioning camera',
    'Performance Testing: Real-world performance, battery usage, and network conditions'
]

for reason in debug_reasons:
    add_bullet_point(doc, reason)

doc.add_paragraph('4.2 Software Requirements').runs[0].bold = True

software_list = [
    'Android Studio (Latest stable version with Android SDK)',
    'JDK 11 (Java Development Kit)',
    'Git (Version control)',
    'Firebase Account (Free tier with Firebase project setup)',
    'Google Cloud Platform account (For Google Maps API key - free tier available)'
]

for software in software_list:
    add_bullet_point(doc, software)

# Page break
doc.add_page_break()

# 5. Architecture
add_colored_heading(doc, '5. Architecture & Design', 1)

doc.add_paragraph(
    'MVP (Model-View-Presenter) Pattern: SAFAR follows strict architectural separation for maintainable, testable code:'
)

arch_components = [
    'Models: Plain Java POJOs (User, Ride, Message, JoinRequest, Report)',
    'Views: Activities/Fragments handling only UI updates',
    'Presenters: Business logic layer connecting Models and Views',
    'Repositories: Data access layer managing all Firebase operations'
]

for component in arch_components:
    add_bullet_point(doc, component)

doc.add_paragraph('Language Distribution:').runs[0].bold = True

lang_notes = [
    'Java: All business logic (Models, Repositories, Presenters, Services)',
    'Kotlin: Some UI components (Activities, Fragments, Adapters)',
    'XML: Layout files, drawables, resources'
]

for note in lang_notes:
    add_bullet_point(doc, note)

doc.add_paragraph('Firebase Schema:').runs[0].bold = True
add_bullet_point(doc, 'Firestore: /users, /rides, /requests, /reports, /ratings')
add_bullet_point(doc, 'Realtime Database: /chats/{rideId}/messages (for instant messaging)')

# 6. Project Status
add_colored_heading(doc, '6. Implementation Status', 1)

status_table = doc.add_table(rows=11, cols=2)
status_table.style = 'Light Grid Accent 1'

# Header
status_hdr = status_table.rows[0].cells
status_hdr[0].text = 'Feature'
status_hdr[1].text = 'Status'
for cell in status_hdr:
    for paragraph in cell.paragraphs:
        for run in paragraph.runs:
            run.font.bold = True
            run.font.color.rgb = RGBColor(255, 214, 0)
    shading_elm = OxmlElement('w:shd')
    shading_elm.set(qn('w:fill'), '0A0A0A')
    cell._element.get_or_add_tcPr().append(shading_elm)

# Data
status_data = [
    ['Phone OTP Authentication', '✅ Completed'],
    ['User Profiles', '✅ Completed'],
    ['Post Ride Advertisements', '✅ Completed'],
    ['Map-Based Ride Discovery', '✅ Completed'],
    ['Join Request System', '✅ Completed'],
    ['Real-Time Chat', '✅ Completed'],
    ['Push Notifications', '✅ Completed'],
    ['Reporting System', '✅ Completed'],
    ['Rating System UI', '🔄 In Progress'],
    ['Testing & Polish', '⏳ Upcoming']
]

for i, (feature, status) in enumerate(status_data, start=1):
    row = status_table.rows[i]
    row.cells[0].text = feature
    row.cells[1].text = status
    for cell in row.cells:
        for paragraph in cell.paragraphs:
            for run in paragraph.runs:
                run.font.size = Pt(10)

doc.add_paragraph()

# 7. Expected Outcomes
add_colored_heading(doc, '7. Expected Outcomes & Benefits', 1)

doc.add_paragraph('For Users:').runs[0].bold = True

user_benefits = [
    'Cost savings through shared transportation',
    'Reduced carbon footprint',
    'Community building through shared travel experiences',
    'More flexible departure times than scheduled transport',
    'Safety through verified users, ratings, and reporting'
]

for benefit in user_benefits:
    add_bullet_point(doc, benefit)

doc.add_paragraph('For Developers/Students:').runs[0].bold = True

dev_benefits = [
    'Hands-on experience with Firebase ecosystem',
    'MVP architecture mastery and clean code principles',
    'Real-world Google Maps SDK integration',
    'Java + Kotlin hybrid project experience',
    'Understanding of real-time systems and push notifications',
    'Portfolio-worthy full-stack mobile development project'
]

for benefit in dev_benefits:
    add_bullet_point(doc, benefit)

# 8. Conclusion
add_colored_heading(doc, '8. Conclusion', 1)

doc.add_paragraph(
    'SAFAR addresses a real transportation gap in Pakistan by providing a simple, trusted, and community-driven carpool marketplace. Built with industry-standard technologies (Java as primary language, Kotlin for UI enhancements, Android Studio IDE, Firebase backend, Google Maps) and following best practices (MVP architecture, Material Design 3), this project demonstrates comprehensive mobile development skills while solving a practical problem.'
)

doc.add_paragraph(
    'The application requires Android Studio for development and a physical debugging phone for real-world testing of GPS, authentication, and notification features. With core features implemented and safety mechanisms in progress, SAFAR is positioned to become a viable solution for intercity carpooling in Pakistan.'
)

# Footer
doc.add_paragraph()
doc.add_paragraph('_' * 80)

footer = doc.add_paragraph('SAFAR — Community Carpool Marketplace')
footer.alignment = WD_ALIGN_PARAGRAPH.CENTER
footer.runs[0].bold = True
footer.runs[0].font.size = Pt(11)

footer2 = doc.add_paragraph('سفر • Connecting Journeys, Building Community')
footer2.alignment = WD_ALIGN_PARAGRAPH.CENTER
footer2.runs[0].font.size = Pt(10)
footer2.runs[0].font.color.rgb = RGBColor(102, 102, 102)

footer3 = doc.add_paragraph('BSCS Final Year Project • SEECS, NUST • Pakistan')
footer3.alignment = WD_ALIGN_PARAGRAPH.CENTER
footer3.runs[0].font.size = Pt(9)
footer3.runs[0].font.color.rgb = RGBColor(102, 102, 102)

# Save document
doc.save('SAFAR_Project_Proposal.docx')
print('✅ Word document created successfully: SAFAR_Project_Proposal.docx')
