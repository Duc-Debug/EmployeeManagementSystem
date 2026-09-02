import { Manrope, Sora } from "next/font/google";

const manrope = Manrope({ subsets: ["latin", "vietnamese"], variable: "--font-manrope" });
const sora = Sora({ subsets: ["latin"], weight: ["600", "700", "800"], variable: "--font-sora" });

export default function RootLayout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="vi" className={`${manrope.variable} ${sora.variable}`}>
        <body className="font-sans">{children}</body>
        </html>
    );
}