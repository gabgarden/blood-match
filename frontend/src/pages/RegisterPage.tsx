import RegisterForm from "../components/RegisterForm";
import { Link } from "react-router-dom";

export default function RegisterPage() {
  return (
    <div className="bg-surface text-on-surface min-h-[100dvh]">

      {/* Header */}
      <header className="sticky top-0 left-0 w-full z-50 flex justify-between items-center px-6 py-4 h-16 bg-[#f9f9fb] backdrop-blur-md bg-opacity-80">
        <div className="text-2xl font-extrabold text-primary headline-font tracking-tight">
          Bloodmatch
        </div>

        <div className="hidden md:flex gap-6 items-center">
        
          <Link to="/login" className="bg-primary text-white px-5 py-2 rounded-full font-bold shadow-sm hover:scale-95 transition-transform">
            Login 
          </Link>
        </div>
      </header>

      {/* Main */}
      <main className="mx-auto w-full max-w-6xl items-start gap-6 px-4 py-4 lg:flex lg:min-h-[calc(100dvh-4rem)] lg:items-center lg:justify-between lg:py-6">

        {/* LEFT SIDE */}
        <div className="hidden flex-1 lg:block">

          <div className="space-y-4">
            <span className="inline-flex items-center gap-2 px-3 py-1 bg-red-100 text-primary rounded-full text-xs font-bold uppercase tracking-widest headline-font">
              <span className="material-symbols-outlined text-sm">
                emergency_home
              </span>
              Participe do Movimento
            </span>

            <h1 className="text-4xl xl:text-5xl font-extrabold headline-font tracking-tighter leading-tight">
              Ajude a salvar <br />
              <span className="text-primary italic">vidas</span> .
            </h1>

            <p className="text-secondary text-sm xl:text-base max-w-md font-light leading-relaxed">
              Conecte-se a um ecossistema de saúde de ponta onde cada gota importa.
               Inicie sua jornada como doador ou coordene requisições que salvam vidas hoje mesmo.
              
            </p>


            <div className="relative max-w-md h-44 xl:h-52 rounded-3xl overflow-hidden shadow-2xl">
              <img
                className="w-full h-full object-cover"
                src="https://lh3.googleusercontent.com/aida-public/AB6AXuCD_LGqamYXUSD3sA_2xaoNPVaOAyGlUi7gxN_auyADZ7NJVEbZQaUyzgwC_GKDdflvbj7nj9e_yctWnFYhVRD10JuN1I_Kmuc2lI_tmE-n8Q8CJQIBeEKfV7Rc4mgviQdKbFqZQKs1Bkhw3yw1YPEtn98r5Y-vmPCrLSHZ3cRcvu3i9xE82s1YdzuT3m61TlLnmUrSa6iKq9gIoBy5_eO1-65oofy7bgNua2SONULoPsbAqb_LSKMPCGKQblIpe7fGYZV8oRMtQS0"
                alt="medical facility"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-red-500/35 to-transparent"></div>
            </div>
          </div>
        </div>

        {/* RIGHT SIDE (FORM CARD) */}
        <div className="w-full max-w-xl lg:ml-8 lg:max-w-lg xl:ml-10 xl:max-w-xl bg-white p-4 lg:p-5 rounded-[2rem] shadow-xl border border-gray-200">

          <div className="space-y-5">

            <div className="text-center">
              <h2 className="text-2xl font-bold headline-font">
                Criar Conta
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Selecione seu tipo de conta e preencha os dados.
              </p>
            </div>

            {/* FORM */}
            <RegisterForm />

            <p className="text-center text-xs text-gray-500 leading-relaxed px-2">
              Ao clicar em registrar, você concorda com nossos{" "}
              <span className="text-primary font-bold underline">
                Termos de Uso
              </span>{" "}
              e{" "}
              <span className="text-primary font-bold underline">
                Política de Privacidade
              </span>
              .
            </p>

          </div>
        </div>
      </main>

      {/* Background Effects */}
      <div className="fixed -bottom-20 -left-20 w-96 h-96 bg-red-500/5 rounded-full blur-[100px] -z-10"></div>
      <div className="fixed top-20 -right-20 w-72 h-72 bg-blue-200/20 rounded-full blur-[80px] -z-10"></div>

    </div>
  );
}