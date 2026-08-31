type AccountCredentialsFieldsProps = {
  email: string;
  password: string;
  confirmPassword: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  inputStyle: string;
  labelStyle: string;
};

export function AccountCredentialsFields({
  email,
  password,
  confirmPassword,
  onChange,
  inputStyle,
  labelStyle,
}: AccountCredentialsFieldsProps) {
  return (
    <section className="space-y-2">
      <h3 className="text-xs font-extrabold uppercase tracking-wider text-gray-700">Credenciais de acesso</h3>

      <div className="flex flex-col">
        <label className={labelStyle}>E-mail</label>
        <input
          name="email"
          type="email"
          placeholder="voce@exemplo.com"
          value={email}
          onChange={onChange}
          className={inputStyle}
          required
        />
      </div>

      <div className="flex flex-col">
        <label className={labelStyle}>Senha</label>
        <input
          name="password"
          type="password"
          placeholder="Digite uma senha segura"
          value={password}
          onChange={onChange}
          className={inputStyle}
          minLength={8}
          required
        />
      </div>

      <div className="flex flex-col">
        <label className={labelStyle}>Confirmação de senha</label>
        <input
          name="confirmPassword"
          type="password"
          placeholder="Repita a senha"
          value={confirmPassword}
          onChange={onChange}
          className={inputStyle}
          minLength={8}
          required
        />
      </div>
    </section>
  );
}
